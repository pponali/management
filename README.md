// Example Rule Configurations

// Margin Rule
{
"ruleName": "Category Margin Rule",
"ruleType": "MARGIN_RULE",
"conditions": {
"categoryId": "ELECTRONICS",
"minimumPrice": 1000,
"attributes": {
"brand_tier": "PREMIUM"
}
},
"actions": {
"type": "MARGIN",
"marginPercentage": 25,
"roundingMethod": "CEIL",
"roundingValue": 9
}
}

// Markdown Rule
{
"ruleName": "Seasonal Markdown",
"ruleType": "MARKDOWN_RULE",
"conditions": {
"categoryId": "APPAREL",
"seasonCode": "SUMMER",
"inventoryAge": {
"moreThan": 60
}
},
"actions": {
"type": "DISCOUNT",
"discountPercentage": 30,
"maxDiscountAmount": 5000
}
}

// Competitive Rule
{
"ruleName": "Competitive Pricing",
"ruleType": "COMPETITIVE_RULE",
"conditions": {
"competitorPrice": {
"available": true,
"source": ["AMAZON", "FLIPKART"]
}
},
"actions": {
"type": "COMPETITIVE",
"matchType": "BEAT",
"differencePercentage": 5,
"minimumMargin": 10
}
}

// Bundle Rule
{
"ruleName": "Bundle Discount",
"ruleType": "BUNDLE_RULE",
"conditions": {
"bundleId": "MOBILE_COMBO",
"minimumQuantity": 2,
"products": ["PHONE", "CASE", "CHARGER"]
},
"actions": {
"type": "BUNDLE_DISCOUNT",
"discountType": "PERCENTAGE",
"value": 15,
"applyTo": "ALL"
}
}