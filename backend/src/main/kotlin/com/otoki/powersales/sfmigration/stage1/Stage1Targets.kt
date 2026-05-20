package com.otoki.powersales.sfmigration.stage1

/**
 * Stage 1 적재 target 메타 카탈로그.
 *
 * 등록되지 않은 target 이름은 controller 에서 404 반환. 신규 entity 적용 시 본 맵에 추가
 * (POC 는 ErpOrderProduct 만).
 */
object Stage1Targets {

    private val ERP_ORDER_PRODUCT = EntityMetadata(
        targetName = "ErpOrderProduct",
        sObjectName = "ERP_OrderProduct__c",
        tableName = "erp_order_product",
        csvFileName = "erp_order_products.csv",
        fields = listOf(
            FieldMapping("Id", "sfid", nullable = false),
            FieldMapping("Name", "name"),
            FieldMapping("ERPOrderId__c", "erp_order_sfid"),
            FieldMapping("SAPOrderNumber__c", "sap_order_number", nullable = false),
            FieldMapping("LineNumber__c", "line_number", nullable = false),
            FieldMapping("ExternalKey__c", "external_key", nullable = false),
            FieldMapping("ProductCode__c", "product_code"),
            FieldMapping("ProductName__c", "product_name"),
            FieldMapping("OrderQuantity__c", "order_quantity"),
            FieldMapping("Unit__c", "unit"),
            FieldMapping("ConfirmQuantity_Box__c", "confirm_quantity_box"),
            FieldMapping("ConfirmQuantity__c", "confirm_quantity"),
            FieldMapping("Confirm_Unit__c", "confirm_unit"),
            FieldMapping("DefaultReason__c", "default_reason"),
            FieldMapping("LineItemStatus__c", "line_item_status"),
            FieldMapping("OrderStatus__c", "delivery_status"),
            FieldMapping("ShippingDriverName__c", "shipping_driver_name"),
            FieldMapping("ShippingVehicle__c", "shipping_vehicle"),
            FieldMapping("ShippingDriverPhone__c", "shipping_driver_phone"),
            FieldMapping("ShippingScheduleTime__c", "shipping_schedule_time"),
            FieldMapping("ShippingCompleteTime__c", "shipping_complete_time"),
            FieldMapping("ShippingQuantity_Box__c", "shipping_quantity_box"),
            FieldMapping("ShippingQuantity__c", "shipping_quantity"),
            FieldMapping("OrderSalesLineAmount__c", "order_sales_line_amount"),
            FieldMapping("ShippingAmount__c", "shipping_amount"),
            FieldMapping("Plant__c", "plant"),
            FieldMapping("Plant_NM__c", "plant_nm"),
            FieldMapping("ReleaseQuantity__c", "release_quantity"),
            FieldMapping("ReleaseAmount__c", "release_amount"),
            FieldMapping("BoxQuantity__c", "box_quantity"),
            FieldMapping("OwnerId", "owner_sfid"),
            FieldMapping("CreatedById", "created_by_sfid"),
            FieldMapping("CreatedDate", "created_at", nullable = false),
            FieldMapping("LastModifiedDate", "updated_at", nullable = false),
            FieldMapping("LastModifiedById", "last_modified_by_sfid"),
            FieldMapping("IsDeleted", "is_deleted"),
        ),
    )

    private val ALL: Map<String, EntityMetadata> = mapOf(
        ERP_ORDER_PRODUCT.targetName to ERP_ORDER_PRODUCT,
    )

    fun get(targetName: String): EntityMetadata? = ALL[targetName]

    fun list(): List<String> = ALL.keys.toList()
}
