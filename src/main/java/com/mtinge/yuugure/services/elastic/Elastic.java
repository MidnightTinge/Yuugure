package com.mtinge.yuugure.services.elastic;

import com.mtinge.TagTokenizer.SyntaxError;
import com.mtinge.TagTokenizer.TagTokenizer;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.PrometheusMetrics;
import com.mtinge.yuugure.data.elastic.EUpload;
import com.mtinge.yuugure.data.elastic.ElasticSearchResult;
import com.mtinge.yuugure.data.postgres.DBTag;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.IService;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Elastic implements IService {
  private static final Logger logger = LoggerFactory.getLogger(Elastic.class);
  private static final String IDX_UPLOADS = "uploads";
  public static final int PAGINATION_SIZE = 30;

  private RestHighLevelClient client;

  @Override
  public void init() throws Exception {
    // elastic starts the server on ctor so don't create until start().
  }

  @Override
  public void start() throws Exception {
    this.client = new RestHighLevelClient(
      RestClient.builder(
        App.config().elastic.nodes.stream()
          .map(node -> new HttpHost(node.host, node.port))
          .toArray(HttpHost[]::new)
      )
    );

    // send a ping to do an initial connection attempt. on fail it will bubble a ConnectException
    client.ping(RequestOptions.DEFAULT);

    try {
      var indexes = client.indices().get(new GetIndexRequest(IDX_UPLOADS), RequestOptions.DEFAULT);
    } catch (ElasticsearchStatusException e) {
      if (e.status().equals(RestStatus.NOT_FOUND)) {
        // need to create indexes
        createIndexes();
      } else {
        throw e;
      }
    }
  }

  @Override
  public void stop() throws Exception {
    this.client.close();
  }

  public List<DBTag> getTagsForUpload(int id) {
    try {
      var request = new GetRequest(IDX_UPLOADS, String.valueOf(id));
      var response = client.get(request, RequestOptions.DEFAULT);

      if (response.isExists()) {
        var upload = EUpload.fromFields(response.getFields());
        if (upload != null && !upload.tags.isEmpty()) {
          return App.database().getTagsById(upload.tags);
        }
      }
    } catch (IOException ioe) {
      logger.error("Caught IOException while fetching tags for upload {}.", id, ioe);
      PrometheusMetrics.ELASTIC_IO_ERRORS.labels("getTagsForUpload").inc();
    } catch (ElasticsearchStatusException e) {
      if (!e.status().equals(RestStatus.NOT_FOUND)) {
        logger.error("Caught elastic exception while fetching tags for upload {}.", id, e);
      }
    }

    return new LinkedList<>();
  }

  public boolean setTagsForUpload(int id, List<Integer> tags) {
    try {
      var request = new UpdateRequest(IDX_UPLOADS, String.valueOf(id))
        .doc(Map.of(
          "tags", tags
        ));
      var resp = client.update(request, RequestOptions.DEFAULT);
      if (resp.status().equals(RestStatus.OK) || resp.status().equals(RestStatus.CREATED)) {
        return true;
      } else {
        logger.warn("Failed to update tags for upload {}. UpdateResponse returned stats {}.", id, resp.status());
      }
    } catch (IOException ioe) {
      logger.error("Caught IOException while setting tags for upload {}.", id, ioe);
      PrometheusMetrics.ELASTIC_IO_ERRORS.labels("setTagsForUpload").inc();
    } catch (ElasticsearchStatusException e) {
      logger.error("Caught elastic exception while setting tags for upload {}.", id, e);
    }

    return false;
  }

  public boolean newUpload(DBUpload upload, List<DBTag> tags) {
    try {
      var doc = Map.of(
        "id", upload.id,
        "tags", tags.stream().map(t -> t.id).collect(Collectors.toList())
      );
      var request = new IndexRequest(IDX_UPLOADS)
        .id(String.valueOf(upload.id))
        .source(doc);
      var resp = client.index(request, RequestOptions.DEFAULT);
      if (resp.status().equals(RestStatus.OK) || resp.status().equals(RestStatus.CREATED)) {
        return true;
      } else {
        logger.warn("Failed to update tags for upload {}. UpdateResponse returned stats {}.", upload.id, resp.status());
      }
    } catch (IOException ioe) {
      logger.error("Caught IOException while setting tags for upload {}.", upload.id, ioe);
      PrometheusMetrics.ELASTIC_IO_ERRORS.labels("newUpload").inc();
    } catch (ElasticsearchStatusException e) {
      logger.error("Caught elastic exception while setting tags for upload {}.", upload.id, e);
    }

    return false;
  }

  public ElasticSearchResult search(BoolQueryBuilder builder, int page) {
    try {
      if (!builder.hasClauses()) {
        return new ElasticSearchResult(1, 1, List.of());
      }

      var req = new SearchRequest(IDX_UPLOADS)
        .source(SearchSourceBuilder.searchSource()
          .size(PAGINATION_SIZE)
          .from(Math.max(page - 1, 0) * PAGINATION_SIZE)
          .query(builder)
        );
      var res = client.search(req, RequestOptions.DEFAULT);

      var hits = new LinkedList<Integer>();
      for (var hit : res.getHits().getHits()) {
        hits.add(Integer.parseInt(hit.getId()));
      }

      double totalDocs = res.getHits().getTotalHits().value;
      if (totalDocs % PAGINATION_SIZE != 0) {
        --totalDocs;
      }
      var max = Math.max(1, Math.ceil(totalDocs / PAGINATION_SIZE));

      return new ElasticSearchResult(page, (int) max, hits);
    } catch (IOException ioe) {
      logger.error("Caught IOException while searching.", ioe);
      PrometheusMetrics.ELASTIC_IO_ERRORS.labels("search").inc();
    } catch (ElasticsearchStatusException e) {
      logger.error("Caught elastic exception while searching.", e);
    }

    return null;
  }

  public ElasticSearchResult search(String query, int page) {
    try {
      return search(App.tagManager().buildQuery(TagTokenizer.parse(query)), page);
    } catch (SyntaxError e) {
      logger.error("Failed to search with user query \"{}\".", query, e);
    }

    return null;
  }

  private void createIndexes() throws IOException {
    logger.info("Creating indexes...");

    var uploadIndex = new CreateIndexRequest(IDX_UPLOADS);
    var mappingBuilder = XContentFactory.jsonBuilder();
    mappingBuilder.startObject();
    {
      mappingBuilder.startObject("properties");
      {
        _property(mappingBuilder, "id", "integer");
        _property(mappingBuilder, "tags", "keyword");
      }
      mappingBuilder.endObject();
    }
    mappingBuilder.endObject();
    uploadIndex.mapping(mappingBuilder);

    client.indices().create(uploadIndex, RequestOptions.DEFAULT);
  }

  private void _property(XContentBuilder builder, String fieldName, String fieldType) throws IOException {
    builder.startObject(fieldName);
    {
      builder.field("type", fieldType);
    }
    builder.endObject();
  }

}
