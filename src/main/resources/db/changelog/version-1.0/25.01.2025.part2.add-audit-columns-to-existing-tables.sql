--liquibase formatted sql

--changeset audit:add-audit-columns-to-user-details
ALTER TABLE user_details 
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN created_by UUID,
ADD COLUMN updated_by UUID;

--changeset audit:add-audit-columns-to-product
ALTER TABLE product 
ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN created_by UUID,
ADD COLUMN updated_by UUID;

--changeset audit:add-audit-columns-to-orders
ALTER TABLE orders 
ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN created_by UUID,
ADD COLUMN updated_by UUID;