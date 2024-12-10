CREATE TABLE IF NOT EXISTS prices (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    base_price DECIMAL(19,2) NOT NULL,
    selling_price DECIMAL(19,2) NOT NULL,
    mrp DECIMAL(19,2) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    currency VARCHAR(3) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    is_seller_active BOOLEAN DEFAULT true,
    is_site_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    
    CONSTRAINT uk_product_seller_site_date UNIQUE (product_id, seller_id, site_id, effective_from)
);
