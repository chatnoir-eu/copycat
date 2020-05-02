package de.webis.cikm20_duplicates.spark;

import java.io.Serializable;
import java.net.URL;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaHadoopRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.trec_ndd.trec_collections.CollectionDocument;
import io.anserini.index.transform.JsoupStringTransform;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

public class SparkCanonicalLinkGraphExtraction {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		String path = "/corpora/corpora-thirdparty/corpus-clueweb/09-mapfile/data-r-*/data";
		
		try (JavaSparkContext context = context()) {
			JavaHadoopRDD<Text, Text> rdd = (JavaHadoopRDD<Text, Text>) context.hadoopFile(path, SequenceFileInputFormat.class, Text.class, Text.class);	
		
			canonicalLinkedges(rdd).saveAsTextFile("cikm2020/canonical-link-graph/cw09");
		}
	}

	private static JavaSparkContext context() {
		SparkConf conf = new SparkConf(true);
		conf.setAppName("cikm2020/canonical-link-graph");
	
		return new JavaSparkContext(conf);
	}
	
	public static JavaRDD<String> canonicalLinkedges(JavaPairRDD<Text, Text> input) {
		return input.map(i -> toVertex(i._1().toString(), i._2().toString()))
				.filter(i -> i != null)
				.map(i -> i.toString());
	}

	@SneakyThrows
	private static CanonicalLinkGraphEdge toVertex(String internalId, String json) {
		// ignore large files
        if (json.getBytes().length > 1024 * 1024) {
            return null;
        }
        
        
        JSONObject inputJson  = new JSONObject(json);

        final JSONObject metadata = inputJson.getJSONObject("metadata");
        if (null == metadata) {
            throw new JSONException("Missing 'metadata'");
        }

        final JSONObject payload = inputJson.getJSONObject("payload");
        if (null == payload) {
            throw new JSONException("Missing 'payload'");
        }

        final String contentBody = payload.getString("body");
        final String contentEncoding    = payload.getString("encoding");

        if (null == contentEncoding || null == contentBody) {
            throw new JSONException("Missing one of 'payload/[encoding|body]'");
        }

        if (!contentEncoding.equals("plain")) {
            return null;
        }
        
        if(!"response".equals(metadata.getString("WARC-Type"))) {
        	return null;
        }

        String targetUri = metadata.getString("WARC-Target-URI");
        URL canonicalLink = extractCanonicalLinkOrNull(targetUri, contentBody);
        if(canonicalLink == null) {
        	return null;
        }
        
        
        String id = internalId;
        if(metadata.has("WARC-TREC-ID")) {
        	id = metadata.getString("WARC-TREC-ID");
        }
        String timestamp = metadata.getString("WARC-Date");
        
        CollectionDocument ret = CollectionDocument.collectionDocument(new JsoupStringTransform().apply(contentBody), id);
        ret.setUrl(new URL(targetUri));
        
		return new CanonicalLinkGraphEdge(ret, canonicalLink, timestamp);
	}
	
	@SneakyThrows
	public static URL extractCanonicalLinkOrNull(String resolveFrom, String contentBody) {
		try {
			Elements canonicals = Jsoup.parse(contentBody).head().select("link[rel=\"canonical\"][href]");
			if(canonicals.size() == 0) {
				return null;
			}
	
			return new URL(new URL(resolveFrom), canonicals.get(0).attr("href"));
		} catch(Exception e) {
			return null;
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@SuppressWarnings("serial")
	public static class CanonicalLinkGraphEdge implements Serializable {
		private CollectionDocument doc;
		private URL canonicalLink;
		private String crawlingTimestamp;
		
		@SneakyThrows
		public String toString() {
			return new ObjectMapper().writeValueAsString(this);
		}
		
		@SneakyThrows
		public static CanonicalLinkGraphEdge fromString(String src) {
			return new ObjectMapper().readValue(src, CanonicalLinkGraphEdge.class);
		}
	}
}