package za.ac.cput.prm_marketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves stored images from disk.
 *
 * <p>Uploaded photographs are the one thing in this API whose address names a file rather than a
 * handler. Mapping the directory here rather than opening files in a controller keeps that route a
 * pure read: no code path takes a path or a name from the caller, and Spring's resource resolver
 * refuses any file it resolves outside this directory, so {@code ../} cannot walk out of it.
 *
 * <p>The location is absolute so the directory does not depend on whichever working directory the
 * process happened to start in. The prefix is the same one {@code ImageUploadServiceImpl} hands
 * back when it stores a file; the two have to agree or a stored image would 404.
 */
@Configuration
public class StorageConfig implements WebMvcConfigurer {

    private static final String UPLOAD_ROOT = "/api/uploads/images/**";

    private final String imagesDirectory;

    public StorageConfig(@Value("${app.storage.images-dir}") String imagesDirectory) {
        this.imagesDirectory = imagesDirectory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path directory = Paths.get(imagesDirectory).toAbsolutePath().normalize();
        registry.addResourceHandler(UPLOAD_ROOT)
                .addResourceLocations("file:" + directory.toString().replace('\\', '/') + "/");
    }
}
