package at.ac.meduniwien.vcfvisualize.security;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Service
public class BucketAccessProvider {

    @SneakyThrows
    public static String generateGoogleBucketGetObjectSignedUrl(String googleCloudCredentials, String googleProjectId, String googleBucketName, String objectName) throws StorageException {
        Storage storage = StorageOptions.newBuilder().setProjectId(googleProjectId).build().getService();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(googleBucketName, objectName)).build();
        URL url = storage.signUrl(blobInfo, 60, TimeUnit.MINUTES, Storage.SignUrlOption.signWith(ServiceAccountCredentials.fromStream(new ByteArrayInputStream(googleCloudCredentials.getBytes()))));
        return url.toString();
    }

    public static String generateSignedUrl(String sample, String path, String token) {
        return "/variant/loadreads/" + sample + "?path=" + path + "&token=" + token;
    }

}
