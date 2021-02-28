package mc.dragons.core.storage.mongo.pagination;

import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

/**
 * Utilities to help with multi-page results parsing.
 * Mostly specific to MongoDB implementation.
 * 
 * @author Adam
 *
 */
public class PaginationUtil {
	public static int pageSkip(int page, int pageSize) {
		return pageSize * (page - 1);
	}
	
	public static <E> List<E> paginateList(List<E> results, int page, int pageSize) {
		int skip = pageSkip(page, pageSize);
		return results.subList(Math.min(skip, results.size() - 1), Math.min(skip + pageSize, results.size() - 1));
	}
	
	public static FindIterable<Document> sortAndPaginate(FindIterable<Document> results, int page, int pageSize) {
		return sortAndPaginate(results, page, pageSize, "_id", false);
	}
	
	public static FindIterable<Document> sortAndPaginate(FindIterable<Document> results, int page, int pageSize, String field, boolean asc) {
		return results.sort(new Document(field, asc ? 1 : -1)).skip(pageSkip(page, pageSize)).limit(pageSize);
	}
}
