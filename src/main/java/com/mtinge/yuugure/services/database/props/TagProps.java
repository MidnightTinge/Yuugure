package com.mtinge.yuugure.services.database.props;

import com.mtinge.yuugure.core.TagManager.TagCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class TagProps {
  /**
   * The tag's ancestor, e.g. "rating:s"'s parent would be "rating:safe"'s ID.
   */
  private Integer parent = null;
  /**
   * The tag's category. For any user-set tag this should be {@link TagCategory#USERLAND}.
   */
  private TagCategory category = null;
  /**
   * The name of the tag.
   */
  private String name = null;
  /**
   * The association type, e.g. "artist".
   */
  private String assocType = null;
  /**
   * The associated ID. If assocType was "artist" then this ID would be the ID of the artist it
   * identifies.
   */
  private Integer assocId = null;
}
