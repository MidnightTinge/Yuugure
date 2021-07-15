package com.mtinge.yuugure.core.TagManager;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.Node;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;
import com.mtinge.TagTokenizer.tokenizer.TagToken;
import com.mtinge.TagTokenizer.tokenizer.TermModifier;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.postgres.DBTag;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A tag management class that interfaces with the <code>tag</code> database table and attemtps to
 * be the source of truth on DB state. Provides tag caching based on name and RadixTree access for
 * wildcard efficiency.
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
          .mapTo(DBTag.class)
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
   * @param descriptor The tag descriptor to create.
   *
   * @return The tag to return.
   */
  public TagCreationResult createTag(TagDescriptor descriptor) {
    return App.database().jdbi().withHandle(handle -> createTag(descriptor, handle));
  }

  /**
   * Create a new tag. Inserts into the database then populates the Radix tree.
   *
   * @param descriptor The tag descriptor.
   * @param handle The database handle to reuse.
   *
   * @return The created tag.
   *
   * @throws IllegalArgumentException if the tag already exists.
   */
  public TagCreationResult createTag(TagDescriptor descriptor, Handle handle) {
    synchronized (_monitor) {
      var exists = getTag(descriptor) != null;
      if (exists) throw new IllegalArgumentException("The requested tag name already exists");

      Objects.requireNonNull(descriptor.category);
      Objects.requireNonNull(descriptor.name);
      if (descriptor.name.isBlank()) {
        throw new IllegalArgumentException("Tag names cannot be blank.");
      }

      var others = getAllFromTree(descriptor.name);
      if (others != null && !others.isEmpty()) {
        boolean foundUserland = false;
        if (others.stream().anyMatch(t -> !t.category.equalsIgnoreCase(TagCategory.USERLAND.name))) {
          // Found a system tag, block creation
          return TagCreationResult.forMessages(List.of(tagConflictRefusal(descriptor.name)));
        }
      }

      var tag = handle.createQuery("INSERT INTO tag (category, name) VALUES (:category, :name) RETURNING *")
        .bind("category", descriptor.category.getName())
        .bind("name", descriptor.name)
        .mapTo(DBTag.class).first();

      addOrAppend(tag);
      return TagCreationResult.forTags(List.of(tag));
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
  public TagCreationResult ensureTag(TagDescriptor descriptor) {
    return App.database().jdbi().withHandle(handle -> ensureTag(descriptor, handle));
  }

  /**
   * Ensures the given TagDescriptor exists in our tree. If it does not, we create it on the
   * database.
   *
   * @param descriptor The tag descriptor.
   * @param handle The database handle to reuse.
   *
   * @return The tag we ensured.
   */
  public TagCreationResult ensureTag(TagDescriptor descriptor, Handle handle) {
    synchronized (_monitor) {
      var existing = getTag(descriptor);
      if (existing == null) {
        return createTag(descriptor, handle);
      }

      return TagCreationResult.forTags(List.of(existing));
    }
  }

  /**
   * Ensures all tags in a mutexed block.
   *
   * @param descriptors The tags to ensure.
   * @param enforceUserlandCreation Whether or not to allow the creation of non-userland tags. If
   *   a tag comes up that doesn't exist and doesn't have a userland category we skip its creation.
   *
   * @return The ensured tags.
   */
  public TagCreationResult ensureAll(List<TagDescriptor> descriptors, boolean enforceUserlandCreation) {
    return App.database().jdbi().inTransaction(handle -> ensureAll(descriptors, enforceUserlandCreation, handle));
  }

  /**
   * Ensures all tags in a mutexed block.
   *
   * @param descriptors The tags to ensure.
   * @param enforceUserlandCreation Whether or not to allow the creation of non-userland tags. If
   *   a tag comes up that doesn't exist and doesn't have a userland category we skip its creation.
   * @param handle The database handle to reuse.
   *
   * @return The ensured tags.
   */
  public TagCreationResult ensureAll(List<TagDescriptor> descriptors, boolean enforceUserlandCreation, Handle handle) {
    var mtx = App.redis().getMutex("tm:ensure");
    try {
      mtx.acquire();

      var result = new TagCreationResult(new LinkedList<>(), new LinkedList<>());
      var created = new LinkedList<DBTag>();

      for (var descriptor : descriptors) {
        if (descriptor.category.equals(TagCategory.USERLAND)) {
          var others = getAllFromTree(descriptor.name);
          if (others != null && !others.isEmpty()) {
            boolean foundUserland = false;
            if (others.stream().anyMatch(t -> !t.category.equalsIgnoreCase(TagCategory.USERLAND.name))) {
              // Found a system tag, block creation
              result.addMessage(tagConflictRefusal(descriptor.name));
              continue;
            }
          }
        }

        var existing = getTag(descriptor);
        if (existing == null) {
          // create the tag, ensuring userland enforcement is respected
          if (!enforceUserlandCreation || descriptor.category.equals(TagCategory.USERLAND)) {
            existing = handle.createQuery("INSERT INTO tag (category, name) VALUES (:category, :name) RETURNING *")
              .bind("category", descriptor.category.name)
              .bind("name", descriptor.name.toLowerCase().trim())
              .mapTo(DBTag.class)
              .first();
            created.add(existing);
          }
        }

        result.addTag(existing);
      }

      // ensure our new tags are in the cache. don't add/append until we're done iterating to avoid
      // cache getting an entry when an exception is thrown
      for (var tag : created) {
        addOrAppend(tag);
      }

      return result;
    } finally {
      mtx.release();
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
        if (tag.category.equalsIgnoreCase(descriptor.category.name)) {
          return tag;
        }
      }
    }

    return null;
  }

  /**
   * Gets all tags matching the given {@code value} regardless of their {@code category}.
   *
   * @param value The tag name to search for.
   *
   * @return The matching tags.
   */
  private List<MutableTag> getAllFromTree(String value) {
    synchronized (_monitor) {
      return tagCache.getValueForExactKey(formatTag(value));
    }
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
              .mapTo(DBTag.class)
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
   * Search the tree recursively for a prefixed/suffixed value. The location of the wildcard denotes
   * which way we search the tree, e.g. for a suffix search we'll go depth-first post-order and
   * compare nodes with the end of our suffix.
   *
   * @param node The current node.
   * @param results The current results.
   * @param search The tag search.
   * @param path The current path.
   */
  @SuppressWarnings({"unchecked", "ConstantConditions"})
  private void _search(Node node, LinkedList<MutableTag> results, TagSearch search, String path) {
    if (node == null) return;

    if (!path.isBlank() && !node.getIncomingEdge().isEmpty()) {
      // this isn't the root node, check if we can short circuit this tree at all
      if (search.prefix != null && search.mode != SearchMode.WRAPPED) {
        var _p = formatTag(path.isBlank() ? node.getIncomingEdge().toString() : path);
        if (!(_p.startsWith(search.prefix) || search.prefix.startsWith(_p))) {
          // short circuit, this branch can't contain our values.
          return;
        }
      }
    }

    for (var outgoing : node.getOutgoingEdges()) {
      _search(outgoing, results, search, path + node.getIncomingEdge());
    }

    if (node.getValue() == null) return;

    var nSuffix = formatTag(node.getIncomingEdge().toString());
    var nPrefix = formatTag(path.isBlank() ? nSuffix : path);
    var nFull = path + nSuffix;

    boolean appendable = false;
    if (search.mode.equals(SearchMode.SUFFIX)) {
      if (nSuffix.endsWith(search.suffix)) {
        appendable = true;
      }
    } else if (search.mode.equals(SearchMode.PREFIX)) {
      if (nSuffix.startsWith(search.prefix) || formatTag(path + nSuffix).startsWith(search.prefix)) {
        appendable = true;
      }
    } else if (search.mode.equals(SearchMode.MIDDLE)) {
      if (nPrefix.startsWith(search.prefix) && nSuffix.endsWith(search.suffix)) {
        appendable = true;
      }
    } else if (search.mode.equals(SearchMode.WRAPPED)) {
      if (nFull.contains(search.prefix)) {
        appendable = true;
      }
    }

    if (appendable) {
      var tags = (LinkedList<MutableTag>) node.getValue();
      if (tags.size() == 1) {
        results.add(tags.getFirst());
      } else {
        // attempt to prefer a system tag if it exists

        var n = tags.getFirst();
        if (n.category.equalsIgnoreCase(TagCategory.USERLAND.name)) {
          for (MutableTag tag : tags) {
            if (!tag.category.equalsIgnoreCase(TagCategory.USERLAND.name)) {
              n = tag;
              break;
            }
          }
        }
        results.add(n);
      }
    }
  }

  /**
   * Searches the tree for the given input. If there is no asterisk then this functions the same as
   * {@link ConcurrentRadixTree#getValueForExactKey(CharSequence)}
   *
   * @param search The search term. If there is an asterisk, then wildcard searching is
   *   attempted.
   *
   * @return The {@link MutableTag}s present for the search hits.
   */
  public LinkedList<MutableTag> search(String search) {
    if (search.isBlank()) {
      return new LinkedList<>();
    }

    if (search.indexOf(' ') > 0) {
      return search(search.substring(0, search.indexOf(' ')));
    }

    var ret = new LinkedList<MutableTag>();

    int idx = search.lastIndexOf('*');
    synchronized (_monitor) {
      if (search.startsWith("*") && search.endsWith("*")) {
        // wrapped search
        var wrapped = formatTag(search.substring(search.indexOf('*') + 1, idx));
        _search(tagCache.getNode(), ret, TagSearch.wrapped(wrapped), "");
      } else if (search.endsWith("*")) {
        // prefix search
        _search(tagCache.getNode(), ret, TagSearch.prefix(formatTag(search.substring(0, search.length() - 1))), "");
      } else if (search.startsWith("*")) {
        // suffix search
        _search(tagCache.getNode(), ret, TagSearch.suffix(formatTag(search.substring(1))), "");
      } else if (idx != -1) {
        // middle search
        var prefix = formatTag(search.substring(0, idx));
        var suffix = formatTag(search.substring(idx + 1));
        _search(tagCache.getNode(), ret, TagSearch.middle(prefix, suffix), "");
      } else {
        var td = TagDescriptor.parse(search);
        if (td != null) {
          var fromTree = tagCache.getValueForExactKey(td.name);
          if (fromTree != null) {
            for (var tag : fromTree) {
              if (tag.category.equalsIgnoreCase(td.category.name)) {
                ret.add(tag);
              }
            }
          }
        } else {
          var fromTree = tagCache.getValueForExactKey(search);
          ret.addAll(fromTree);
        }
      }
    }
    return ret;
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

    if (child.parent != null && (child.parent == parent.id)) {
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

  /**
   * Removes a tag association.
   *
   * @param descriptor The tag to remove ancestors of.
   *
   * @return Whether or not we updated the tag.
   */
  public boolean removeParent(TagDescriptor descriptor) {
    var tag = getFromTree(descriptor);
    if (tag == null) {
      logger.warn("Attempted to removeParent on a non-existant tag \"{}:{}\".", descriptor.name, descriptor.category);
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

  private void _injectAs(BoolQueryBuilder builder, MutableTag tag, TermModifier as) {
    if (tag.parent != null && tag.parent > 0) {
      if (as.equals(TermModifier.NOT)) {
        builder.mustNot(QueryBuilders.termQuery("tags", tag.id));
        builder.mustNot(QueryBuilders.termQuery("tags", tag.parent));
      } else {
        builder.should(QueryBuilders.termQuery("tags", tag.id));
        builder.should(QueryBuilders.termQuery("tags", tag.parent));
      }
    } else {
      var term = QueryBuilders.termQuery("tags", tag.id);
      switch (as) {
        case AND -> builder.must(term);
        case NOT -> builder.mustNot(term);
        case OR -> builder.should(term);
      }
    }
  }

  @SuppressWarnings("ConstantConditions")
  private void _query(BoolQueryBuilder builder, List<TagToken> tokens) {
    for (var token : tokens) {
      if (token.type.equals(TagToken.Type.GROUP)) {
        var mapped = buildQuery(token.children);

        switch (token.modifier) {
          case AND -> builder.must(mapped);
          case NOT -> builder.mustNot(mapped);
          case OR -> builder.should(mapped);
        }
      } else if (token.type.equals(TagToken.Type.TERM)) {
        var tags = search(token.value.toLowerCase().trim());
        if (tags.size() > 1) {
          // Since we have more than one result we're assuming this was from a wildcard result.
          // We don't want our wildcard terms to be 'MUST' because we're looking for any of the
          // following tags, but we also want to respect a 'NOT' wildcard search.

          // Generate a secondary bool query of 'OR' to inject
          var wrapped = QueryBuilders.boolQuery();
          for (var tag : tags) {
            wrapped.should(QueryBuilders.termQuery("tags", tag.id));
          }
          switch (token.modifier) {
            case NOT -> builder.mustNot(wrapped);
            case OR -> builder.should(wrapped);
            case AND -> builder.must(wrapped);
          }
        } else if (!tags.isEmpty()) {
          _injectAs(builder, tags.getFirst(), token.modifier);
        }
      }
    }
  }

  /**
   * Returns a {@link BoolQueryBuilder} equivilent to the provided tokenized input for elastic
   * searching.
   *
   * @param tokens The tokens to map.
   *
   * @return The mapped tokens.
   *
   * @see com.mtinge.yuugure.services.elastic.Elastic#search(BoolQueryBuilder, int)
   */
  public BoolQueryBuilder buildQuery(List<TagToken> tokens) {
    var builder = new BoolQueryBuilder();
    _query(builder, tokens);

    return builder;
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

  private String tagConflictRefusal(String tagName) {
    return "Refused to create tag \"" + tagName + "\" due to conflicting system tag of the same name.";
  }
}
