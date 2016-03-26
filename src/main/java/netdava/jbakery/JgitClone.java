package netdava.jbakery;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class JgitClone {

    public static void main(String[] args) throws IOException {
        String gitRepo = "ssh://ieugen@git.netdava.com:29418/netdava/website.git";

        String fileName = "website";


        // this is necessary when the remote host does not have a valid certificate, ideally we would install the certificate in the JVM
        // instead of this unsecure workaround!
        CredentialsProvider allowHosts = new CredentialsProvider() {

            @Override
            public boolean supports(CredentialItem... items) {
                for (CredentialItem item : items) {
                    if ((item instanceof CredentialItem.YesNoType)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
                for (CredentialItem item : items) {
                    if (item instanceof CredentialItem.YesNoType) {
                        ((CredentialItem.YesNoType) item).setValue(true);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean isInteractive() {
                return false;
            }
        };

        // prepare a new folder for the cloned repository
        File localPath = new File(fileName);
        localPath.mkdirs();
        localPath.delete();

        // then clone
        System.out.println("Cloning from " + gitRepo + " to " + localPath);
        try (Git result = Git.cloneRepository()
                .setCredentialsProvider(allowHosts)
//                .setCredentialsProvider(CredentialsProvider.getDefault())
                .setURI(gitRepo)
                .setDirectory(localPath)
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            System.out.println("Having repository: " + result.getRepository().getDirectory());
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

}
