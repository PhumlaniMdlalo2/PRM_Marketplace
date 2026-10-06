package za.ac.cput.prm_marketplace.dto;

/**
 * The address a freshly stored image can be read back from.
 *
 * <p>Relative on purpose: the form that uploads the file, and every page that renders the result,
 * is served by the same origin as the API, so a scheme and host here would only go stale the
 * moment the API moved behind a proxy.
 */
public record ImageUploadResponse(String url) {
}
