package com.scaler.price.core.management.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceDTO {
    private Long id;
    private String productId;
    private String sellerId;
    private String siteId;
    private BigDecimal basePrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private String priceType;
    private String currency;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isActive;
    private SellerAttributesDTO sellerAttributes;
    private SiteAttributesDTO siteAttributes;
    private String status;

    public static PriceDTOBuilder builder() {
        return new PriceDTOBuilder();
    }

    public static class PriceDTOBuilder {
        private Long id;
        private String productId;
        private String sellerId;
        private String siteId;
        private BigDecimal basePrice;
        private BigDecimal sellingPrice;
        private BigDecimal mrp;
        private String priceType;
        private String currency;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Boolean isActive;
        private SellerAttributesDTO sellerAttributes;
        private SiteAttributesDTO siteAttributes;
        private String status;

        PriceDTOBuilder() {
            // Set default values
            this.isActive = true;
            this.effectiveFrom = LocalDateTime.now();
            this.status = "ACTIVE";
        }

        public PriceDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PriceDTOBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public PriceDTOBuilder sellerId(String sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public PriceDTOBuilder siteId(String siteId) {
            this.siteId = siteId;
            return this;
        }

        public PriceDTOBuilder basePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
            return this;
        }

        public PriceDTOBuilder basePrice(String basePrice) {
            if (basePrice != null && !basePrice.trim().isEmpty()) {
                this.basePrice = new BigDecimal(basePrice.trim());
            }
            return this;
        }

        public PriceDTOBuilder sellingPrice(BigDecimal sellingPrice) {
            this.sellingPrice = sellingPrice;
            return this;
        }

        public PriceDTOBuilder sellingPrice(String sellingPrice) {
            if (sellingPrice != null && !sellingPrice.trim().isEmpty()) {
                this.sellingPrice = new BigDecimal(sellingPrice.trim());
            }
            return this;
        }

        public PriceDTOBuilder mrp(BigDecimal mrp) {
            this.mrp = mrp;
            return this;
        }

        public PriceDTOBuilder mrp(String mrp) {
            if (mrp != null && !mrp.trim().isEmpty()) {
                this.mrp = new BigDecimal(mrp.trim());
            }
            return this;
        }

        public PriceDTOBuilder priceType(String priceType) {
            this.priceType = priceType;
            return this;
        }

        public PriceDTOBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PriceDTOBuilder effectiveFrom(LocalDateTime effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public PriceDTOBuilder effectiveFrom(String effectiveFrom) {
            if (effectiveFrom != null && !effectiveFrom.trim().isEmpty()) {
                this.effectiveFrom = LocalDateTime.parse(effectiveFrom.trim());
            }
            return this;
        }

        public PriceDTOBuilder effectiveTo(LocalDateTime effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public PriceDTOBuilder effectiveTo(String effectiveTo) {
            if (effectiveTo != null && !effectiveTo.trim().isEmpty()) {
                this.effectiveTo = LocalDateTime.parse(effectiveTo.trim());
            }
            return this;
        }

        public PriceDTOBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public PriceDTOBuilder sellerAttributes(SellerAttributesDTO sellerAttributes) {
            this.sellerAttributes = sellerAttributes;
            return this;
        }

        public PriceDTOBuilder siteAttributes(SiteAttributesDTO siteAttributes) {
            this.siteAttributes = siteAttributes;
            return this;
        }

        public PriceDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PriceDTO build() {
            validatePrice();

            PriceDTO priceDTO = new PriceDTO();
            priceDTO.setId(this.id);
            priceDTO.setProductId(this.productId);
            priceDTO.setSellerId(this.sellerId);
            priceDTO.setSiteId(this.siteId);
            priceDTO.setBasePrice(this.basePrice);
            priceDTO.setSellingPrice(this.sellingPrice);
            priceDTO.setMrp(this.mrp);
            priceDTO.setPriceType(this.priceType);
            priceDTO.setCurrency(this.currency);
            priceDTO.setEffectiveFrom(this.effectiveFrom);
            priceDTO.setEffectiveTo(this.effectiveTo);
            priceDTO.setIsActive(this.isActive);
            priceDTO.setSellerAttributes(this.sellerAttributes);
            priceDTO.setSiteAttributes(this.siteAttributes);
            priceDTO.setStatus(this.status);

            return priceDTO;
        }

        private void validatePrice() {
            StringBuilder errors = new StringBuilder();

            if (productId == null || productId.trim().isEmpty()) {
                errors.append("Product ID is required. ");
            }
            if (sellerId == null || sellerId.trim().isEmpty()) {
                errors.append("Seller ID is required. ");
            }
            if (siteId == null || siteId.trim().isEmpty()) {
                errors.append("Site ID is required. ");
            }
            if (basePrice == null) {
                errors.append("Base Price is required. ");
            } else if (basePrice.compareTo(BigDecimal.ZERO) <= 0) {
                errors.append("Base Price must be greater than zero. ");
            }
            if (sellingPrice == null) {
                errors.append("Selling Price is required. ");
            } else if (sellingPrice.compareTo(BigDecimal.ZERO) <= 0) {
                errors.append("Selling Price must be greater than zero. ");
            }
            if (mrp == null) {
                errors.append("MRP is required. ");
            } else if (mrp.compareTo(BigDecimal.ZERO) <= 0) {
                errors.append("MRP must be greater than zero. ");
            }
            if (currency == null || currency.trim().isEmpty()) {
                errors.append("Currency is required. ");
            }
            if (effectiveFrom == null) {
                errors.append("Effective From date is required. ");
            }
            if (priceType == null || priceType.trim().isEmpty()) {
                errors.append("Price Type is required. ");
            }

            // Business rule validations
            if (basePrice != null && mrp != null && basePrice.compareTo(mrp) > 0) {
                errors.append("Base Price cannot be greater than MRP. ");
            }
            if (sellingPrice != null && mrp != null && sellingPrice.compareTo(mrp) > 0) {
                errors.append("Selling Price cannot be greater than MRP. ");
            }
            if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
                errors.append("Effective From date must be before Effective To date. ");
            }

            if (errors.length() > 0) {
                throw new IllegalStateException("Invalid Price: " + errors.toString());
            }
        }
    }
}
