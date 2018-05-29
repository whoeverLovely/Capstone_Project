package com.louise.udacity.mydict;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;


public class GCSTest {

    @Test
    public void downloadTest() {
        Storage storage = StorageOptions.newBuilder()
                .setProjectId("ipod")
                .build()
                .getService();

        BlobId blobId = BlobId.of("udacity_mydict", "gre_list");
        Blob blob = storage.get(blobId);

        if (blob == null) {
            System.out.println("No such object");
            return;
        }

        Path downloadTo = FileSystems.getDefault().getPath("test");
        PrintStream writeTo = System.out;
        try {
            if (downloadTo != null) {

                writeTo = new PrintStream(new FileOutputStream(downloadTo.toFile()));

            }
            if (blob.getSize() < 1_000_000) {
                // Blob is small read all its content in one request
                byte[] content = blob.getContent();
                writeTo.write(content);
            } else {
                // When Blob size is big or unknown use the blob's channel reader.
                try (ReadChannel reader = blob.reader()) {
                    WritableByteChannel channel = Channels.newChannel(writeTo);
                    ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
                    while (reader.read(bytes) > 0) {
                        bytes.flip();
                        channel.write(bytes);
                        bytes.clear();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (downloadTo == null) {
            writeTo.println();
        } else {
            writeTo.close();
        }
    }
}

