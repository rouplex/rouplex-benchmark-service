package org.rouplex.service.benchmark.auth;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.Gson;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.service.benchmark.BenchmarkConfigurationKey;

import java.util.ArrayList;

// Work in progress, not used for now
public class AuthProviders {
    private static final Gson gson = new Gson();

    private final String s3BucketName;
    private final String s3ProfilesPrefix;
    private final AmazonS3 s3Client;

    AuthProviders(Configuration configuration) throws Exception {
        String s3Folder = configuration.get(BenchmarkConfigurationKey.BenchmarkUserProfilesS3Url);
        int bucketSeparatorIndex = s3Folder.indexOf("/", "s3://".length());
        s3BucketName = s3Folder.substring("s3://".length(), bucketSeparatorIndex);
        s3ProfilesPrefix = s3Folder.substring(bucketSeparatorIndex + 1);

        s3Client = AmazonS3Client.builder().build();

        for (String provider : list(s3ProfilesPrefix)) {

        }
    }

    /**
     * Poor man's implementation, improve when having many entries
     *
     * @param prefix
     * @return
     */
    Iterable<String> list(String prefix) {
        ArrayList<String> items = new ArrayList<>();
        String marker = null;

        do {
            ObjectListing objectListing = s3Client.listObjects(
                    new ListObjectsRequest(s3BucketName, prefix, marker, "/", 1000));

            for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                items.add(s3ObjectSummary.getKey());
            }
            marker = objectListing.getNextMarker();
        } while (marker != null);

        return items;
    }
}
