package com.mtinge.yuugure.core.TagManager;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.postgres.DBTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * A tag management class that interfaces with the <code>tag</code> table and attemtps to be the
 * source of truth on DB state. Provides tag caching based on name and RadixTree access for wildcard
 * efficiency.
 */
public class TagManager {
  private static final Logger logger = LoggerFactory.getLogger(TagManager.class);

  private final Object _monitor = new Object();
  private ConcurrentRadixTree<LinkedList<MutableTag>> tagCache;

  public TagManager() {
    this.tagCache = new ConcurrentRadixTree<>(new DefaultCharSequenceNodeFactory());
  }

  /**
   * <p>Resets our RadixTree to match the Database state.</p>
   * <p><strong>WARNING:</strong> Locks the <code>tag</code> table in
   * <strong>ACCESS EXCLUSIVE</strong> mode.</p>
   */
  public void reload() {
    synchronized (_monitor) {
      logger.info("Reloading...");
      var toSet = new ConcurrentRadixTree<LinkedList<MutableTag>>(new DefaultCharSequenceNodeFactory());
      App.database().jdbi().useHandle(handle -> {
        handle.begin();
        // warning: Access Exclusive is a heavy table-based lock that should be used sparingly. It
        //          is used here because our RadixTree needs to be a source of truth on the
        //          database state.
        handle.execute("LOCK TABLE tag IN ACCESS EXCLUSIVE MODE");

        handle.createQuery("SELECT * FROM tag WHERE true")
          .map(DBTag.Mapper)
          .stream()
          .forEach(tag -> addOrAppend(toSet, tag));

        handle.commit();
      });
      this.tagCache = toSet; // only swap memory when we've completely structured the tree
      logger.info("Done.");
    }
  }

  /**
   * Create a new tag. Inserts into the database then populates the Radix tree.
   *
   * @param descriptor The tag descriptor.
   *
   * @return The created tag.
   *
   * @throws IllegalArgumentException if the tag already exists.
   */
  public DBTag createTag(TagDescriptor descriptor) {
    synchronized (_monitor) {
      var exists = getTag(descriptor) != null;
      if (exists) throw new IllegalArgumentException("The requested tag name already exists");

      var tag = App.database().jdbi().withHandle(handle ->
        handle.createQuery("INSERT INTO tag (name, type) VALUES (:name, :type) RETURNING *")
          .bind("name", descriptor.name)
          .bind("type", descriptor.type.getType())
          .map(DBTag.Mapper)
          .first()
      );

      addOrAppend(tag);
      return tag;
    }
  }

  /**
   * Ensures the given TagDescriptor exists in our tree. If it does not, we create it on the
   * database.
   *
   * @param descriptor The tag descriptor.
   *
   * @return The tag we ensured.
   */
  public DBTag ensureTag(TagDescriptor descriptor) {
    synchronized (_monitor) {
      var existing = getTag(descriptor);
      if (existing == null) {
        existing = createTag(descriptor);
      }
      return existing;
    }
  }

  /**
   * Gets a tag from the Radix tree with the specified name.
   *
   * @param descriptor The tag descriptor.
   *
   * @return The {@link DBTag} associated with this name, if it exists. Null otherwise.
   */
  public DBTag getTag(TagDescriptor descriptor) {
    var fromTree = getFromTree(descriptor);
    if (fromTree != null) {
      return fromTree.toDb();
    }

    return null;
  }

  /**
   * Gets a mutable tag from our cache.
   *
   * @param descriptor The descriptor to fetch.
   *
   * @return The mutable tag if it exists, null otherwise.
   */
  private MutableTag getFromTree(TagDescriptor descriptor) {
    List<MutableTag> fromCache;
    synchronized (_monitor) {
      fromCache = tagCache.getValueForExactKey(formatTag(descriptor.name));
    }

    if (fromCache != null) {
      for (var tag : fromCache) {
        if (tag.type.equalsIgnoreCase(descriptor.type.type)) {
          return tag;
        }
      }
    }

    return null;
  }

  /**
   * Deletes a tag. Unlinks it from uploads in the database and deletes the tag itself.
   *
   * @param descriptor The tag descriptor.
   *
   * @return Whether or not the tag was purged.
   */
  public boolean deleteTag(TagDescriptor descriptor) {
    // This is potential over synchronization especially since we have a long-running blocking task
    // in the DB but I need the tagCache to be the source of truth to minimize DB hits.
    synchronized (_monitor) {
      var existing = getTag(descriptor);

      if (existing != null) {
        var purged = App.database().jdbi().withHandle(handle -> {
          handle.begin();
          try {
            var fromDb = handle.createQuery("SELECT * FROM tag WHERE id = :tid FOR UPDATE")
              .bind("tid", existing.id)
              .map(DBTag.Mapper)
              .findFirst().orElse(null);
            if (fromDb != null) {
              handle.createQuery("SELECT 1 FROM upload_tags WHERE tag = :tid FOR UPDATE")
                .bind("tid", existing.id)
                .execute((r, c) -> null);

              handle.createUpdate("DELETE FROM upload_tags WHERE tag = :tid")
                .bind("tid", fromDb.id)
                .execute();
              var tagsDeleted = handle.createUpdate("DELETE FROM tag WHERE id = :tid")
                .bind("tid", fromDb.id)
                .execute();

              if (tagsDeleted == 0) {
                logger.warn("No tags deleted from the table, rolling back.");
                handle.rollback();
                return false;
              } else {
                handle.commit();
                return true;
              }
            } else {
              logger.warn("Attempted to delete tag {} but it does not exist in the database.", existing.id);
              return false;
            }
          } catch (Exception e) {
            logger.error("Failed to delete tag {} ({}).", existing.id, descriptor.name, e);
            handle.rollback();
            return false;
          }
        });

        if (purged) {
          tagCache.remove(formatTag(descriptor.name));
          return true;
        }
      }

      return false;
    }
  }

  /**
   * <p>Gets a list of tags that start with the provided wildcard suffix. Wildcard must include the
   * asterisk (*) to denote the search term. Search terms (the substring before the asterisk) must
   * be at least 1 char or they will be discarded silently and an empty list will be returned.</p>
   * <p>Valid:
   * <pre>TagManager.getWildcard("search*");</pre>
   * <pre>TagManager.getWildcard("to*");</pre></p>
   *
   * <p>Invalid:
   * <pre>TagManager.getWildcard("search"); // No asterisk</pre>
   * <pre>TagManager.getWildcard("*");* // Too short</pre></p>
   *
   * @param wildcard The search string. Must include an asterisk and the length before the
   *   asterisk must be >= 1.
   *
   * @return The list of tags.
   *
   * @throws IllegalArgumentException if the search does not include an asterisk.
   */
  public LinkedList<MutableTag> getWildcard(String wildcard) {
    var toRet = new LinkedList<MutableTag>();

    int idx = wildcard.lastIndexOf('*');
    if (idx == -1) {
      throw new IllegalArgumentException("The provided wildcard does not include an asterisk.");
    } else if (idx >= 1) {
      synchronized (_monitor) {
        var prefixedValues = tagCache.getValuesForKeysStartingWith(formatTag(wildcard.substring(0, idx)));
        for (var values : prefixedValues) {
          toRet.addAll(values);
        }
      }
    } // else: wildcard too short to care about. discard.

    return toRet;
  }

  /**
   * Sets the {@code child}'s parent to {@code parent}.
   *
   * @param childDescriptor The child to update.
   * @param parentDescriptor The parent to set as the child's parent.
   *
   * @return Whether or not the query updated successfully.
   */
  public boolean setParent(TagDescriptor childDescriptor, TagDescriptor parentDescriptor) {
    var parent = getFromTree(parentDescriptor);
    var child = getFromTree(childDescriptor);

    if (parent == null || child == null) {
      return false;
    }

    return App.database().jdbi().withHandle(handle -> {
      handle.begin();
      try {
        handle.execute("SELECT parent FROM tag WHERE id = ? FOR UPDATE", child.id);

        var updated = handle.createUpdate("UPDATE tag SET parent = :pid WHERE id = :cid")
          .bind("pid", parent.id)
          .bind("cid", child.id)
          .execute();

        if (updated > 0) {
          handle.commit();
          child.parent = parent.id;
          return true;
        } else {
          logger.warn("No rows updated when setting child {}'s parent to {}.", child.id, parent.id);
          handle.rollback();
          return false;
        }
      } catch (Exception e) {
        logger.error("Failed to set associations for parent {} and child {}.", parent.id, child.id, e);
        handle.rollback();
      }
      return false;
    });
  }

  public boolean removeParent(TagDescriptor descriptor) {
    var tag = getFromTree(descriptor);
    if (tag == null) {
      logger.warn("Attempted to removeParent on a non-existant tag \"{}:{}\".", descriptor.name, descriptor.type);
      return false;
    } else {
      return App.database().jdbi().withHandle(handle -> {
        handle.begin();
        try {
          handle.execute("SELECT 1 FROM tag WHERE id = ? FOR UPDATE", tag.id);

          var updated = handle.createUpdate("UPDATE tag SET parent = null WHERE id = :id")
            .bind("id", tag.id)
            .execute();

          if (updated > 0) {
            handle.commit();
            tag.parent = null;
            return true;
          } else {
            handle.rollback();
            return false;
          }
        } catch (Exception e) {
          logger.error("Failed to remove child {}'s parent.", tag.id, e);
          handle.rollback();
        }

        return false;
      });
    }
  }

  private void addOrAppend(DBTag tag) {
    addOrAppend(this.tagCache, tag);
  }

  private void addOrAppend(ConcurrentRadixTree<LinkedList<MutableTag>> tree, DBTag tag) {
    var name = formatTag(tag.name);

    var list = tree.getValueForExactKey(name);
    if (list == null) {
      // we have to put a new list
      list = new LinkedList<>(List.of(MutableTag.fromDb(tag)));
      tree.put(name, list);
    } else {
      // mutate the existing list
      list.add(MutableTag.fromDb(tag));
    }
  }

  @SuppressWarnings("unchecked")
  private void _traverse(Node node, List<MutableTag> tags, String path) {
    if (node == null) return;
    for (var n : node.getOutgoingEdges()) {
      _traverse(n, tags, path + n.getIncomingEdge());
    }

    if (node.getValue() == null) return;
    tags.addAll((LinkedList<MutableTag>) node.getValue());
  }

  /**
   * Walks the Radix tree and collects stored {@link DBTag}s. Walks the tree each iteration so cache
   * results as needed.
   *
   * @return The {@link DBTag}s stored in our RadixTree.
   */
  public List<MutableTag> getTags() {
    var ret = new LinkedList<MutableTag>();
    _traverse(this.tagCache.getNode(), ret, "");

    return ret;
  }

  private String formatTag(String tag) {
    return tag.toLowerCase().trim();
  }
}
