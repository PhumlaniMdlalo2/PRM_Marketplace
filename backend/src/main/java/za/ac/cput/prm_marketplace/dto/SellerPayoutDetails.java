package za.ac.cput.prm_marketplace.dto;

public record SellerPayoutDetails(
        String accountHolder,
        String bankName,
        String accountNumber,
        String branchCode,
        String accountType
) {
}
