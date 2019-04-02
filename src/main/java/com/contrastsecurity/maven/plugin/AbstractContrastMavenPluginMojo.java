package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.models.AgentType;
import com.contrastsecurity.models.Applications;
import com.contrastsecurity.sdk.ContrastSDK;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

abstract class AbstractContrastMavenPluginMojo extends AbstractMojo {

    @Component
    protected MavenProject project;

    @Parameter(property = "username", required = true)
    protected String username;

    @Parameter(property = "apiKey", required = true)
    protected String apiKey;

    @Parameter(property = "serviceKey", required = true)
    protected String serviceKey;

    @Parameter(property = "apiUrl")
    protected String apiUrl;

    @Parameter(property = "orgUuid", required = true)
    protected String orgUuid;

    @Parameter(property = "appName")
    protected String appName;

    @Parameter(property = "appId")
    protected String appId;

    @Parameter(property = "standalone")
    protected boolean standalone;

    @Parameter(property = "minSeverity", defaultValue = "Medium")
    protected String minSeverity;

    @Parameter(property = "serverName", required = true)
    protected String serverName;

    @Parameter(property = "serverPath")
    protected String serverPath;

    @Parameter(property = "jarPath")
    protected String jarPath;

    @Parameter(property = "profile")
    protected String profile;

    @Parameter(property = "appVersion")
    protected String appVersion;

    @Parameter(property = "skipArgLine")
    protected boolean skipArgLine;

    protected String contrastAgentLocation;

    protected String computedAppVersion;
    protected String computedAppName;
    protected ContrastSDK contrastSdk;

    private static final String AGENT_NAME = "contrast.jar";

    public void execute() throws MojoExecutionException {
    }

    public void init() throws MojoExecutionException {
        verifyAppIdOrNameNotBlank();
        getLog().info("Connecting to Contrast");
        contrastSdk = connectToTeamServer();
        computedAppName = computeAppName(contrastSdk);
        computedAppVersion = computeAppVersion(new Date());
    }

    public String computeAppName(ContrastSDK sdk) throws MojoExecutionException {

        if (computedAppName != null) {
            return computedAppName;
        }

        // If they set an appName and not an appId, then we're done
        if(StringUtils.isNotBlank(appName) && StringUtils.isBlank(appId)) {
            return appName;
        }

        // If they specified both an appName and an appId,
        // warn them that we're just going to use appId
        if(StringUtils.isNotBlank(appName) && StringUtils.isNotBlank(appId)) {
            getLog().warn("\n\nYou specified an appId and an appName in the plugin setup");
            getLog().warn("We're going to ignore the appName and use the appId to find you application");
        }

        //If we've made it here, then the user specified an appId
        //Let's get the appName from Contrast
        if (StringUtils.isNotBlank(appId)) {
            return getAppNameFromContrast(sdk, appId);
        }

        throw new MojoExecutionException("\n\nCould not figure out what appName to use");
    }

    public String computeAppVersion(Date currentDate) {
        if (computedAppVersion != null) {
            return computedAppVersion;
        }

        if (appVersion != null) {
            getLog().info("Using user-specified app version [" + appVersion + "]");
            computedAppVersion = appVersion;
            return computedAppVersion;
        }

        String travisBuildNumber = System.getenv("TRAVIS_BUILD_NUMBER");
        String circleBuildNum = System.getenv("CIRCLE_BUILD_NUM");

        String appVersionQualifier = "";
        if(travisBuildNumber != null) {
            getLog().info("Build is running in TravisCI. We'll use TRAVIS_BUILD_NUMBER [" + travisBuildNumber + "]");
            appVersionQualifier = travisBuildNumber;
        } else if (circleBuildNum != null) {
            getLog().info("Build is running in CircleCI. We'll use CIRCLE_BUILD_NUM [" + circleBuildNum + "]");
            appVersionQualifier = circleBuildNum;
        } else {
            getLog().info("No CI build number detected, we'll use current timestamp.");
            appVersionQualifier = new SimpleDateFormat("yyyyMMddHHmmss").format(currentDate);
        }
        if (StringUtils.isNotBlank(appId)) {
            computedAppVersion = computedAppName + "-" + appVersionQualifier;
        } else {
            computedAppVersion = computedAppName + "-" + appVersionQualifier;
        }

        return computedAppVersion;
    }

    ContrastSDK connectToTeamServer() throws MojoExecutionException {
        try {
            if (!StringUtils.isEmpty(apiUrl)) {
                return new ContrastSDK(username, serviceKey, apiKey, apiUrl);
            } else {
                return new ContrastSDK(username, serviceKey, apiKey);
            }
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("\n\nWe couldn't connect to TeamServer at this address [" + apiUrl + "]. The error is: ", e);
        }
    }

    String getAppNameFromContrast(ContrastSDK contrastSDK, String applicationId) throws MojoExecutionException {
        Applications applications;
        try {
            applications = contrastSDK.getApplication(orgUuid, applicationId);
        } catch (Exception e) {
            String logMessage;
            if (e.getMessage().contains("403")) {
                logMessage = "\n\n Unable to find the application on TeamServer with the id [" + applicationId + "]\n";
            } else {
                logMessage = "\n\n Unable to retrieve the application list from TeamServer. Please check that TeamServer is running at this address [" + apiUrl + "]\n";
            }
            throw new MojoExecutionException(logMessage, e);
        }
        if (applications.getApplication() == null) {
            throw new MojoExecutionException("\n\nApplication with id '" + applicationId + "' not found. Make sure this application appears in TeamServer under the 'Applications' tab.\n");
        }
        return applications.getApplication().getName();
    }

    public void verifyAppIdOrNameNotBlank() throws MojoExecutionException {
        if (StringUtils.isBlank(appId) && StringUtils.isBlank(appName)) {
            throw new MojoExecutionException("Please specify appId or appName in the plugin configuration.");
        }
    }

    File installJavaAgent(ContrastSDK connection) throws MojoExecutionException {
        byte[] javaAgent;
        File agentFile;

        if (StringUtils.isEmpty(jarPath)) {
            getLog().info("No jar path was configured. Downloading the latest contrast.jar...");

            try {
                if (profile != null) {
                    javaAgent = connection.getAgent(AgentType.JAVA, orgUuid, profile);
                } else {
                    javaAgent = connection.getAgent(AgentType.JAVA, orgUuid);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("\n\nWe couldn't download the Java agent from TeamServer with this user [" + username + "]. Please check that all your credentials are correct. If everything is correct, please contact Contrast Support. The error is:", e);
            } catch (UnauthorizedException e) {
                throw new MojoExecutionException("\n\nWe contacted TeamServer successfully but couldn't authorize with the credentials you provided. The error is:", e);
            }

            // Save the jar to the 'target' directory
            agentFile = new File(project.getBuild().getDirectory() + File.separator + AGENT_NAME);

            try {
                FileUtils.writeByteArrayToFile(agentFile, javaAgent);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to save the latest java agent.", e);
            }

            getLog().info("Saved the latest java agent to " + agentFile.getAbsolutePath());
            contrastAgentLocation = agentFile.getAbsolutePath();

        } else {
            getLog().info("Using configured jar path " + jarPath);

            agentFile = new File(jarPath);

            if (!agentFile.exists()) {
                throw new MojoExecutionException("Unable to load the local Java agent from " + jarPath);
            }

            getLog().info("Loaded the latest java agent from " + jarPath);
            contrastAgentLocation = jarPath;

        }

        return agentFile;
    }
}