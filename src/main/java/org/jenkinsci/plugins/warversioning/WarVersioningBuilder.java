package org.jenkinsci.plugins.warversioning;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 * 
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link WarVersioningBuilder} is created. The created instance is persisted to
 * the project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #rename}) to remember the configuration.
 * 
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 * 
 * @author Kohsuke Kawaguchi
 */
public class WarVersioningBuilder extends Builder {

    private final boolean rename;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public WarVersioningBuilder(boolean rename) {
        this.rename = rename;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public boolean getRename() {
        return rename;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
            final BuildListener listener) {
        try {
            FilePath[] warFiles = build.getWorkspace().list("**/*.war");

            final String buildNumber = Integer.toString(build.getNumber());

            for (FilePath filePath : warFiles) {
                filePath.act(new FileCallable<Void>() {
                    private static final long serialVersionUID = 1L;

                    public Void invoke(File f, VirtualChannel channel)
                            throws IOException, InterruptedException {
                        if (StringUtils.contains(f.getName(), "##")) {
                            return null;
                        }

                        Path hardlink = createVersioningWarPath(f, buildNumber);

                        listener.getLogger().println(
                                "Create link " + hardlink.toFile().getName()
                                        + " to " + f.getName());

                        if (rename) {
                            Files.move(f.toPath(), hardlink,
                                    StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.createLink(hardlink, f.toPath());
                        }

                        return null;
                    }
                });
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    private Path createVersioningWarPath(File f, String buildNumber) {
        String name = f.getName();

        String basename = FilenameUtils.getBaseName(name);
        String ext = FilenameUtils.getExtension(name);

        String fullname = String
                .format("%s##%s.%s", basename, buildNumber, ext);

        File hardlink = new File(f.getParent(), fullname);
        return hardlink.toPath();
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link WarVersioningBuilder}. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
     * 
     * <p>
     * See
     * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.WarVersioning_DisplayName();
        }
    }
}
