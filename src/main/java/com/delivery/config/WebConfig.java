package com.delivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * Custom Web MVC Configurer to expose the uploaded files directory
 * to the web container as a static resource path.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${upload.profile.dir:uploads/profile-images}")
    private String profileUploadDir;

    @Value("${upload.document.dir:uploads/documents}")
    private String docUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the local directory for bills
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString();
        
        // Ensure path ends with slash
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }

        // Add resource handler for bills
        registry.addResourceHandler("/uploads/bills/**")
                .addResourceLocations("file:" + absolutePath);

        // Expose the local directory for profiles
        String profileAbsolutePath = Paths.get(profileUploadDir).toAbsolutePath().toString().replace("\\", "/");
        if (!profileAbsolutePath.endsWith("/")) {
            profileAbsolutePath += "/";
        }

        // Add resource handler for profile photos
        registry.addResourceHandler("/uploads/profile-images/**")
                .addResourceLocations("file:" + profileAbsolutePath);

        // Expose the local directory for documents
        String docAbsolutePath = Paths.get(docUploadDir).toAbsolutePath().toString();
        if (!docAbsolutePath.endsWith(File.separator)) {
            docAbsolutePath += File.separator;
        }

        // Add resource handler for documents
        registry.addResourceHandler("/uploads/documents/**")
                .addResourceLocations("file:" + docAbsolutePath);
    }
}
