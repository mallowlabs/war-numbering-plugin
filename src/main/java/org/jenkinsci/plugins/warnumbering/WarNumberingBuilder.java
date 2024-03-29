package org.jenkinsci.plugins.warnumbering;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

/**
 * War Numbering {@link Builder} .
 */
public class WarNumberingBuilder extends Builder {
    /** Output type: hardlink */
    private static final String OUTPUT_TYPE_HARDLINK = "hardlink";
    /** Output type: move */
    private static final String OUTPUT_TYPE_RENAME = "rename";

    /** output type option. */
    private String outputType;

    /**
     * Constructor
     *
     * @param outputType
     *            output type option
     */
    @DataBoundConstructor
    public WarNumberingBuilder(String outputType) {
        this.outputType = outputType;
    }

    /**
     * output type option.
     *
     * @return 'hardlink' or 'move'
     */
    public String getOutputType() {
        return outputType;
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

                        Path hardlink = createNumberingWarPath(f, buildNumber);

                        listener.getLogger().println(
                                "Create link " + hardlink.toFile().getName()
                                        + " to " + f.getName());

                        if (OUTPUT_TYPE_RENAME.equals(getOutputType())) {
                            Files.move(f.toPath(), hardlink,
                                    StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.createLink(hardlink, f.toPath());
                        }

                        return null;
                    }

					@Override
					public void checkRoles(RoleChecker checker) throws SecurityException {
						// do nothing
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

    private Path createNumberingWarPath(File f, String buildNumber) {
        String name = f.getName();

        String basename = FilenameUtils.getBaseName(name);
        String ext = FilenameUtils.getExtension(name);

        String fullname = String
                .format("%s##%s.%s", basename, buildNumber, ext);

        File hardlink = new File(f.getParent(), fullname);
        return hardlink.toPath();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link WarNumberingBuilder}. Used as a singleton. The
     * class is marked as public so that it can be accessed from views.
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
            return Messages.WarNumbering_DisplayName();
        }
    }
}
