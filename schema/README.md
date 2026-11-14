# SLML Schema Reference

## Schema File
- **File**: `layout-schema.xsd`
- **Repository URL**: https://raw.githubusercontent.com/Syren-Dev-Tech/slml/refs/heads/main/schema/layout-schema.xsd
- **Target Namespace**: `https://syren-dev-tech.com/slml`

## Using the Schema in Your XML Files

### Proper Namespace Declaration

To use the schema in your layout XML files, declare the namespace correctly:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<part id="my_layout" 
      width="100%" 
      height="100%" 
      xmlns="https://syren-dev-tech.com/slml"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="https://syren-dev-tech.com/slml https://raw.githubusercontent.com/Syren-Dev-Tech/slml/refs/heads/main/schema/layout-schema.xsd">
    <!-- Your layout content -->
</part>
```

### Key Namespace Components

1. **Default namespace** (required):
   ```xml
   xmlns="https://syren-dev-tech.com/slml"
   ```
   This declares the SLML namespace as the default, so all elements are in this namespace.

2. **XML Schema Instance** (for validation):
   ```xml
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   ```
   This allows schema validation attributes.

3. **Schema Location** (for IDE validation and external validation):
   ```xml
   xsi:schemaLocation="https://syren-dev-tech.com/slml https://raw.githubusercontent.com/Syren-Dev-Tech/slml/refs/heads/main/schema/layout-schema.xsd"
   ```
   This tells validators where to find the schema file.

## Schema Fixes Applied

### Issue: Missing Namespace Prefix Declaration
- **Problem**: The schema used the `slml:` prefix for type and element references throughout, but the prefix wasn't declared.
- **Error**: XML validators reported warnings about undefined prefixes and invalid attributes.
- **Solution**: Added `xmlns:slml="https://syren-dev-tech.com/slml"` to the schema root element.

### Issue: Incorrect Namespace in Documentation Example
- **Problem**: The example in schema documentation used an incorrect namespace URL.
- **Error**: Users following the example would get validation errors.
- **Solution**: Updated the example to use the correct namespace and GitHub raw URL.

## Supported Elements

The schema defines support for:

- **`<part>`** - Generic container for grouping UI elements
- **`<text>`** - Text display with placeholder support
- **`<button>`** - Interactive buttons (standard or vanilla Minecraft style)
- **`<item>`** - Minecraft item references
- **`<repeated>`** - Dynamic list rendering from data sources
- **`<datasource>`** - Data loading from JSON in data packs
- **`<layout>`** - Reusable component inclusion

## Common Attributes

### Positioning
- `x`, `y`, `width`, `height` - Position and size (pixels or percentages)

### Flex Layout
- `flex` - Direction: `row` (horizontal) or `column` (vertical)
- `align` - Alignment: `flex-start`, `center`, `even`, or `between`
- `flex-grow` - Growth factor for filling space (non-negative integer)

### Styling
- `style` - Semicolon-separated CSS-like properties
  - Colors: `backgroundColor`, `borderColor`, `color` (format: `#RRGGBB`)
  - Sizing: `fontSize`, `padding`, `margin`, `borderWidth` (format: integer)
  - Text: `textAlign`, `fontWeight`, `fontStyle`

### Identification
- `id` - Unique identifier (required for referencing from Java code)

## Placeholder Support

Use `{{placeholder_name}}` for dynamic values:

```xml
<text>Welcome {{player_name}}</text>
<item>{{trade_output}}</item>
```

Types:
- **Runtime placeholders**: Set from Java code
- **Data placeholders**: Properties from datasources (only in `<repeated>` elements)

## IDE Configuration

### VS Code with XML Extension

Add to `.vscode/settings.json`:

```json
{
  "xml.catalogs": [
    {
      "systemId": "https://syren-dev-tech.com/slml",
      "uri": "https://raw.githubusercontent.com/Syren-Dev-Tech/slml/refs/heads/main/schema/layout-schema.xsd"
    }
  ]
}
```

### IntelliJ IDEA

1. Go to Settings → Languages & Frameworks → Schemas and DTDs → XML Schema Mappings
2. Add new mapping:
   - URI: `https://syren-dev-tech.com/slml`
   - Schema file/URL: `https://raw.githubusercontent.com/Syren-Dev-Tech/slml/refs/heads/main/schema/layout-schema.xsd`

## Validation

The schema enforces:

1. **Proper element hierarchy** - Elements can only contain specific child elements
2. **Required attributes** - `id` is required for most elements
3. **Attribute value constraints**:
   - Dimensions: digits with optional `%`
   - Colors: `#RRGGBB` hex format
   - Flex values: specific enumerated values
   - IDs: alphanumeric and underscores only
4. **Data integrity**:
   - Datasource references must exist
   - Layout component names must be valid
   - Placeholder format validation

## Troubleshooting

### Warning: Invalid Attributes
- **Cause**: Namespace not properly declared in XML file
- **Fix**: Use the namespace declaration from "Proper Namespace Declaration" section

### Warning: Missing Prefix "slml"
- **Cause**: Schema validation couldn't find namespace prefix
- **Fix**: Ensure `xmlns="https://syren-dev-tech.com/slml"` is in your root element

### Schema File Not Found
- **Cause**: IDE can't reach the GitHub URL
- **Fix**: Configure local schema fallback in your IDE settings or use relative path if schema is local

## Version History

- **v2.0**: Added datasource element for data pack integration, fixed namespace declarations
