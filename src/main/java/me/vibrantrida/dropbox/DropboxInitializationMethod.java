package me.vibrantrida.dropbox;

import java.io.*;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;

import com.chrismin13.additionsapi.AdditionsAPI;
import com.chrismin13.additionsapi.utils.Debug;

import com.dropbox.core.v2.sharing.ListSharedLinksResult;
import com.dropbox.core.v2.sharing.RequestedVisibility;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;
import org.apache.commons.codec.digest.DigestUtils;
import us.fihgu.toolbox.resourcepack.ResourcePackManager;

public class DropboxInitializationMethod {
    public static String resourcePack;
    public static String resourcePackHash;

    private static DbxRequestConfig dropboxConfig;
    private static DbxClientV2 dropboxClient;

    public static void uploadResourcePack(File f, String dropboxToken) {
        dropboxConfig = DbxRequestConfig.newBuilder("dropbox/additionsapi").build();
        dropboxClient = new DbxClientV2(dropboxConfig, dropboxToken);

        if (ResourcePackManager.neededRebuild) {
            try (InputStream in = new FileInputStream(f.getAbsolutePath())) {
                FileMetadata metadata = dropboxClient.files().uploadBuilder("/" + f.getName())
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in);

                // check for any existing shared links
                ListSharedLinksResult sharedLinksResult = dropboxClient.sharing().listSharedLinksBuilder()
                        .withPath("/" + f.getName())
                        .withDirectOnly(true)
                        .start();

                if (sharedLinksResult.getLinks().size() > 0) {
                    resourcePack = sharedLinksResult.getLinks().get(0).getUrl();
                } else {
                    // shared link doesn't exist, create one
                    SharedLinkSettings linkSettings = SharedLinkSettings.newBuilder()
                            .withRequestedVisibility(RequestedVisibility.PUBLIC)
                            .build();
                    SharedLinkMetadata sharedLinkMetadata = dropboxClient.sharing()
                            .createSharedLinkWithSettings(metadata.getPathLower(), linkSettings);

                    resourcePack = sharedLinkMetadata.getUrl();
                }

                // change url to direct url
                if (resourcePack.endsWith("0")) {
                    resourcePack = resourcePack.substring(0, resourcePack.length() - 1) + "1";
                }

                resourcePackHash = DigestUtils.sha1Hex(in);
            } catch (DbxException | IOException e) {
                e.printStackTrace();
            }
        } else {
            resourcePack = AdditionsAPI.getInstance().getConfig().getString("resource-pack.link");
            resourcePackHash = AdditionsAPI.getInstance().getConfig().getString("resource-pack.sha1");
        }
    }
}
