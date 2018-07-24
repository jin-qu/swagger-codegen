package io.swagger.codegen.v3;

import com.github.jknack.handlebars.Handlebars;
import com.samskivert.mustache.Mustache.Compiler;
import io.swagger.codegen.languages.helpers.HasHelper;
import io.swagger.codegen.languages.helpers.HasNotHelper;
import io.swagger.codegen.languages.helpers.IsHelper;
import io.swagger.codegen.languages.helpers.IsNotHelper;
import io.swagger.codegen.v3.utils.ModelUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ByteArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.EmailSchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import io.swagger.v3.oas.models.parameters.CookieParameter;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ONLY_READ_ONLY_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.HAS_OPTIONAL_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.HAS_REQUIRED_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ARRAY_MODEL_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.CodegenHelper.getDefaultIncludes;
import static io.swagger.codegen.v3.CodegenHelper.getImportMappings;
import static io.swagger.codegen.v3.CodegenHelper.getTypeMappings;
import static io.swagger.codegen.v3.CodegenHelper.initalizeSpecialCharacterMapping;
import static io.swagger.codegen.languages.helpers.ExtensionHelper.getBooleanValue;
import static io.swagger.codegen.v3.utils.ModelUtils.processCodegenModels;
import static io.swagger.codegen.v3.utils.ModelUtils.processModelEnums;
import static io.swagger.codegen.v3.utils.ModelUtils.updateCodegenPropertyEnum;

public class DefaultCodegen implements CodegenConfig {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultCodegen.class);
    public static final String DEFAULT_CONTENT_TYPE = "application/json";
    public static final String REQUEST_BODY_NAME = "body";

    protected String inputSpec;
    protected String outputFolder = "";
    protected Set<String> defaultIncludes = new HashSet<String>();
    protected Map<String, String> typeMapping = new HashMap<String, String>();
    protected Map<String, String> instantiationTypes = new HashMap<String, String>();
    protected Set<String> reservedWords = new HashSet<String>();
    protected Set<String> languageSpecificPrimitives = new HashSet<String>();
    protected Map<String, String> importMapping = new HashMap<String, String>();
    protected String modelPackage = "", apiPackage = "", fileSuffix;
    protected String modelNamePrefix = "", modelNameSuffix = "";
    protected String testPackage = "";
    protected Map<String, String> apiTemplateFiles = new HashMap<String, String>();
    protected Map<String, String> modelTemplateFiles = new HashMap<String, String>();
    protected Map<String, String> apiTestTemplateFiles = new HashMap<String, String>();
    protected Map<String, String> modelTestTemplateFiles = new HashMap<String, String>();
    protected Map<String, String> apiDocTemplateFiles = new HashMap<String, String>();
    protected Map<String, String> modelDocTemplateFiles = new HashMap<String, String>();
    protected Map<String, String> reservedWordsMappings = new HashMap<String, String>();
    protected String templateDir;
    protected String embeddedTemplateDir;
    protected String commonTemplateDir = "_common";
    protected String templateVersion;
    protected Map<String, Object> additionalProperties = new HashMap<String, Object>();
    protected Map<String, Object> vendorExtensions = new HashMap<String, Object>();
    protected List<SupportingFile> supportingFiles = new ArrayList<SupportingFile>();
    protected List<CliOption> cliOptions = new ArrayList<CliOption>();
    protected boolean skipOverwrite;
    protected boolean removeOperationIdPrefix;
    protected boolean supportsInheritance;
    protected boolean supportsMixins;
    protected Map<String, String> supportedLibraries = new LinkedHashMap<String, String>();
    protected String library;
    protected Boolean sortParamsByRequiredFlag = true;
    protected Boolean ensureUniqueParams = true;
    protected Boolean allowUnicodeIdentifiers = false;
    protected String gitUserId, gitRepoId, releaseNote;
    protected String httpUserAgent;
    protected Boolean hideGenerationTimestamp = true;
    // How to encode special characters like $
    // They are translated to words like "Dollar" and prefixed with '
    // Then translated back during JSON encoding and decoding
    protected Map<String, String> specialCharReplacements = new HashMap<String, String>();
    // When a model is an alias for a simple type
    protected Map<String, String> typeAliases = null;

    protected String ignoreFilePathOverride;

    public List<CliOption> cliOptions() {
        return cliOptions;
    }

    public void processOpts() {
        if (additionalProperties.containsKey(CodegenConstants.TEMPLATE_DIR)) {
            this.setTemplateDir((String) additionalProperties.get(CodegenConstants.TEMPLATE_DIR));
        }

        if (additionalProperties.containsKey(CodegenConstants.TEMPLATE_VERSION)) {
            this.setTemplateVersion((String) additionalProperties.get(CodegenConstants.TEMPLATE_VERSION));
        }

        if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            this.setModelPackage((String) additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
        }

        if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            this.setApiPackage((String) additionalProperties.get(CodegenConstants.API_PACKAGE));
        }

        if (additionalProperties.containsKey(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG)) {
            this.setSortParamsByRequiredFlag(Boolean.valueOf(additionalProperties
                    .get(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG).toString()));
        }

        if (additionalProperties.containsKey(CodegenConstants.ENSURE_UNIQUE_PARAMS)) {
            this.setEnsureUniqueParams(Boolean.valueOf(additionalProperties
                    .get(CodegenConstants.ENSURE_UNIQUE_PARAMS).toString()));
        }

        if (additionalProperties.containsKey(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS)) {
            this.setAllowUnicodeIdentifiers(Boolean.valueOf(additionalProperties
                    .get(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS).toString()));
        }

        if(additionalProperties.containsKey(CodegenConstants.MODEL_NAME_PREFIX)){
            this.setModelNamePrefix((String) additionalProperties.get(CodegenConstants.MODEL_NAME_PREFIX));
        }

        if(additionalProperties.containsKey(CodegenConstants.MODEL_NAME_SUFFIX)){
            this.setModelNameSuffix((String) additionalProperties.get(CodegenConstants.MODEL_NAME_SUFFIX));
        }

        if (additionalProperties.containsKey(CodegenConstants.REMOVE_OPERATION_ID_PREFIX)) {
            this.setSortParamsByRequiredFlag(Boolean.valueOf(additionalProperties
                    .get(CodegenConstants.REMOVE_OPERATION_ID_PREFIX).toString()));
        }
    }

    // override with any special post-processing for all models
    @SuppressWarnings({ "static-method", "unchecked" })
    public Map<String, Object> postProcessAllModels(Map<String, Object> processedModels) {
        if (supportsInheritance) {
            // Index all CodegenModels by model name.
            Map<String, CodegenModel> allModels = new HashMap<>();
            for (Map.Entry<String, Object> entry : processedModels.entrySet()) {
                String modelName = toModelName(entry.getKey());
                Map<String, Object> inner = (Map<String, Object>) entry.getValue();
                List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
                for (Map<String, Object> mo : models) {
                    CodegenModel cm = (CodegenModel) mo.get("model");
                    allModels.put(modelName, cm);
                }
            }
            processCodegenModels(allModels);;
        }
        return processedModels;
    }

    // override with any special post-processing
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        return objs;
    }

    /**
     * post process enum defined in model's properties
     *
     * @param objs Map of models
     * @return maps of models with better enum support
     */
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        processModelEnums(objs);
        return objs;
    }

    /**
     * Return the enum default value in the language specified format
     *
     * @param value enum variable name
     * @param datatype data type
     * @return the default value for the enum
     */
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "." + value;
    }

    /**
     * Return the enum value in the language specified format
     * e.g. status becomes "status"
     *
     * @param value enum variable name
     * @param datatype data type
     * @return the sanitized value for enum
     */
    public String toEnumValue(String value, String datatype) {
        if ("number".equalsIgnoreCase(datatype)) {
            return value;
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    /**
     * Return the sanitized variable name for enum
     *
     * @param value enum variable name
     * @param datatype data type
     * @return the sanitized variable name for enum
     */
    public String toEnumVarName(String value, String datatype) {
        return ModelUtils.toEnumVarName(value);
    }

    // override with any special post-processing
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        return objs;
    }

    // override with any special post-processing
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        return objs;
    }

    // override with any special post-processing
    @SuppressWarnings("static-method")
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        return objs;
    }

    // override to post-process any model properties
    @SuppressWarnings("unused")
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property){
    }

    // override to post-process any parameters
    @SuppressWarnings("unused")
    public void postProcessParameter(CodegenParameter parameter){
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
    }

    @Override
    public void processOpenAPI(OpenAPI openAPI) {
    }

    // override with any special handling of the JMustache compiler
    @SuppressWarnings("unused")
    public Compiler processCompiler(Compiler compiler) {
        return compiler;
    }

    // override with any special text escaping logic
    @SuppressWarnings("static-method")
    public String escapeText(String input) {
        if (input == null) {
            return input;
        }

        // remove \t, \n, \r
        // replace \ with \\
        // replace " with \"
        // outter unescape to retain the original multi-byte characters
        // finally escalate characters avoiding code injection
        return escapeUnsafeCharacters(
                StringEscapeUtils.unescapeJava(
                        StringEscapeUtils.escapeJava(input)
                                .replace("\\/", "/"))
                        .replaceAll("[\\t\\n\\r]"," ")
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\""));
    }

    /**
     * override with any special text escaping logic to handle unsafe
     * characters so as to avoid code injection
     * @param input String to be cleaned up
     * @return string with unsafe characters removed or escaped
     */
    public String escapeUnsafeCharacters(String input) {
        LOGGER.warn("escapeUnsafeCharacters should be overridden in the code generator with proper logic to escape " +
                "unsafe characters");
        // doing nothing by default and code generator should implement
        // the logic to prevent code injection
        // later we'll make this method abstract to make sure
        // code generator implements this method
        return input;
    }

    /**
     * Escape single and/or double quote to avoid code injection
     * @param input String to be cleaned up
     * @return string with quotation mark removed or escaped
     */
    public String escapeQuotationMark(String input) {
        LOGGER.warn("escapeQuotationMark should be overridden in the code generator with proper logic to escape " +
                "single/double quote");
        return input.replace("\"", "\\\"");
    }

    public Set<String> defaultIncludes() {
        return defaultIncludes;
    }

    public Map<String, String> typeMapping() {
        return typeMapping;
    }

    public Map<String, String> instantiationTypes() {
        return instantiationTypes;
    }

    public Set<String> reservedWords() {
        return reservedWords;
    }

    public Set<String> languageSpecificPrimitives() {
        return languageSpecificPrimitives;
    }

    public Map<String, String> importMapping() {
        return importMapping;
    }

    public String testPackage() {
        return testPackage;
    }

    public String modelPackage() {
        return modelPackage;
    }

    public String apiPackage() {
        return apiPackage;
    }

    public String fileSuffix() {
        return fileSuffix;
    }

    public String templateDir() {
        return templateDir;
    }

    public String embeddedTemplateDir() {
        if (embeddedTemplateDir != null) {
            return embeddedTemplateDir;
        } else {
            return templateDir;
        }
    }

    public String getCommonTemplateDir() {
        return this.commonTemplateDir;
    }

    public void setCommonTemplateDir(String commonTemplateDir) {
        this.commonTemplateDir = commonTemplateDir;
    }

    public Map<String, String> apiDocTemplateFiles() {
        return apiDocTemplateFiles;
    }

    public Map<String, String> modelDocTemplateFiles() {
        return modelDocTemplateFiles;
    }

    public Map<String, String> reservedWordsMappings() {
        return reservedWordsMappings;
    }

    public Map<String, String> apiTestTemplateFiles() {
        return apiTestTemplateFiles;
    }

    public Map<String, String> modelTestTemplateFiles() {
        return modelTestTemplateFiles;
    }

    public Map<String, String> apiTemplateFiles() {
        return apiTemplateFiles;
    }

    public Map<String, String> modelTemplateFiles() {
        return modelTemplateFiles;
    }

    public String apiFileFolder() {
        return outputFolder + "/" + apiPackage().replace('.', '/');
    }

    public String modelFileFolder() {
        return outputFolder + "/" + modelPackage().replace('.', '/');
    }

    public String apiTestFileFolder() {
        return outputFolder + "/" + testPackage().replace('.', '/');
    }

    public String modelTestFileFolder() {
        return outputFolder + "/" + testPackage().replace('.', '/');
    }

    public String apiDocFileFolder() {
        return outputFolder;
    }

    public String modelDocFileFolder() {
        return outputFolder;
    }

    public Map<String, Object> additionalProperties() {
        return additionalProperties;
    }

    public Map<String, Object> vendorExtensions() {
        return vendorExtensions;
    }

    public List<SupportingFile> supportingFiles() {
        return supportingFiles;
    }

    public String outputFolder() {
        return outputFolder;
    }

    public void setOutputDir(String dir) {
        this.outputFolder = dir;
    }

    public String getOutputDir() {
        return outputFolder();
    }

    public String getInputSpec() {
        return inputSpec;
    }

    public void setInputSpec(String inputSpec) {
        this.inputSpec = inputSpec;
    }

    public void setTemplateDir(String templateDir) {
        this.templateDir = templateDir;
    }

    @Override
    public String getTemplateVersion() {
        return templateVersion;
    }

    public void setTemplateVersion(String templateVersion) {
        this.templateVersion = templateVersion;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public void setModelNamePrefix(String modelNamePrefix){
        this.modelNamePrefix = modelNamePrefix;
    }

    public void setModelNameSuffix(String modelNameSuffix){
        this.modelNameSuffix = modelNameSuffix;
    }

    public void setApiPackage(String apiPackage) {
        this.apiPackage = apiPackage;
    }

    public void setSortParamsByRequiredFlag(Boolean sortParamsByRequiredFlag) {
        this.sortParamsByRequiredFlag = sortParamsByRequiredFlag;
    }

    public void setEnsureUniqueParams(Boolean ensureUniqueParams) {
        this.ensureUniqueParams = ensureUniqueParams;
    }

    public void setAllowUnicodeIdentifiers(Boolean allowUnicodeIdentifiers) {
        this.allowUnicodeIdentifiers = allowUnicodeIdentifiers;
    }

    /**
     * Return the regular expression/JSON schema pattern (http://json-schema.org/latest/json-schema-validation.html#anchor33)
     *
     * @param pattern the pattern (regular expression)
     * @return properly-escaped pattern
     */
    public String toRegularExpression(String pattern) {
        return addRegularExpressionDelimiter(escapeText(pattern));
    }

    /**
     * Return the file name of the Api Test
     *
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    public String toApiFilename(String name) {
        return toApiName(name);
    }

    /**
     * Return the file name of the Api Documentation
     *
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    /**
     * Return the file name of the Api Test
     *
     * @param name the file name of the Api
     * @return the file name of the Api
     */
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    /**
     * Return the variable name in the Api
     *
     * @param name the varible name of the Api
     * @return the snake-cased variable name
     */
    public String toApiVarName(String name) {
        return snakeCase(name);
    }

    /**
     * Return the capitalized file name of the model
     *
     * @param name the model name
     * @return the file name of the model
     */
    public String toModelFilename(String name) {
        return initialCaps(name);
    }

    /**
     * Return the capitalized file name of the model test
     *
     * @param name the model name
     * @return the file name of the model
     */
    public String toModelTestFilename(String name) {
        return initialCaps(name) + "Test";
    }

    /**
     * Return the capitalized file name of the model documentation
     *
     * @param name the model name
     * @return the file name of the model
     */
    public String toModelDocFilename(String name) {
        return initialCaps(name);
    }

    /**
     * Return the operation ID (method name)
     *
     * @param operationId operation ID
     * @return the sanitized method name
     */
    @SuppressWarnings("static-method")
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        return operationId;
    }

    /**
     * Return the variable name by removing invalid characters and proper escaping if
     * it's a reserved word.
     *
     * @param name the variable name
     * @return the sanitized variable name
     */
    public String toVarName(String name) {
        if (reservedWords.contains(name)) {
            return escapeReservedWord(name);
        } else {
            return name;
        }
    }

    /**
     * Return the parameter name by removing invalid characters and proper escaping if
     * it's a reserved word.
     *
     * @param name Codegen property object
     * @return the sanitized parameter name
     */
    public String toParamName(String name) {
        name = removeNonNameElementToCamelCase(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        if (reservedWords.contains(name)) {
            return escapeReservedWord(name);
        }
        return name;
    }

    /**
     * Return the Enum name (e.g. StatusEnum given 'status')
     *
     * @param property Codegen property
     * @return the Enum name
     */
    @SuppressWarnings("static-method")
    public String toEnumName(CodegenProperty property) {
        return StringUtils.capitalize(property.name) + "Enum";
    }

    /**
     * Return the escaped name of the reserved word
     *
     * @param name the name to be escaped
     * @return the escaped reserved word
     *
     * throws Runtime exception as reserved word is not allowed (default behavior)
     */
    @SuppressWarnings("static-method")
    public String escapeReservedWord(String name) {
        throw new RuntimeException("reserved word " + name + " not allowed");
    }

    /**
     * Return the fully-qualified "Model" name for import
     *
     * @param name the name of the "Model"
     * @return the fully-qualified "Model" name for import
     */
    public String toModelImport(String name) {
        if ("".equals(modelPackage())) {
            return name;
        } else {
            return modelPackage() + "." + name;
        }
    }

    /**
     * Return the fully-qualified "Api" name for import
     *
     * @param name the name of the "Api"
     * @return the fully-qualified "Api" name for import
     */
    public String toApiImport(String name) {
        return apiPackage() + "." + name;
    }

    /**
     * Default constructor.
     * This method will map between Swagger type and language-specified type, as well as mapping
     * between Swagger type and the corresponding import statement for the language. This will
     * also add some language specified CLI options, if any.
     *
     *
     * returns string presentation of the example path (it's a constructor)
     */
    public DefaultCodegen() {
        defaultIncludes = getDefaultIncludes();

        typeMapping = getTypeMappings();

        instantiationTypes = new HashMap<String, String>();

        reservedWords = new HashSet<String>();

        importMapping = getImportMappings();

        // we've used the .swagger-codegen-ignore approach as
        // suppportingFiles can be cleared by code generator that extends
        // the default codegen, leaving the commented code below for
        // future reference
        //supportingFiles.add(new GlobalSupportingFile("LICENSE", "LICENSE"));

        cliOptions.add(CliOption.newBoolean(CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG,
                CodegenConstants.SORT_PARAMS_BY_REQUIRED_FLAG_DESC).defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.ENSURE_UNIQUE_PARAMS, CodegenConstants
                .ENSURE_UNIQUE_PARAMS_DESC).defaultValue(Boolean.TRUE.toString()));

        // name formatting options
        cliOptions.add(CliOption.newBoolean(CodegenConstants.ALLOW_UNICODE_IDENTIFIERS, CodegenConstants
                .ALLOW_UNICODE_IDENTIFIERS_DESC).defaultValue(Boolean.FALSE.toString()));

        // initialize special character mapping
        initalizeSpecialCharacterMapping(specialCharReplacements);
    }

    /**
     * Return the symbol name of a symbol
     *
     * @param input Symbol (e.g. $)
     * @return Symbol name (e.g. Dollar)
     */
    protected String getSymbolName(String input) {
        return specialCharReplacements.get(input);
    }

    /**
     * Return the example path
     *
     * @param path the path of the operation
     * @param operation Swagger operation object
     * @return string presentation of the example path
     */
    @SuppressWarnings("static-method")
    public String generateExamplePath(String path, Operation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);

        if (operation.getParameters() != null) {
            int count = 0;

            for (Parameter param : operation.getParameters()) {
                if (param instanceof QueryParameter) {
                    StringBuilder paramPart = new StringBuilder();
                    QueryParameter queryParameter = (QueryParameter) param;

                    if (count == 0) {
                        paramPart.append("?");
                    } else {
                        paramPart.append(",");
                    }
                    count += 1;
                    if (!param.getRequired()) {
                        paramPart.append("[");
                    }
                    paramPart.append(param.getName()).append("=");
                    paramPart.append("{");

                    if (queryParameter.getStyle() != null) {
                        paramPart.append(param.getName()).append("1");
                        if (Parameter.StyleEnum.FORM.equals(queryParameter.getStyle())) {
                            if (queryParameter.getExplode() != null && queryParameter.getExplode()) {
                                paramPart.append(",");
                            } else {
                                paramPart.append("&").append(param.getName()).append("=");
                                paramPart.append(param.getName()).append("2");
                            }
                        }
                        else if (Parameter.StyleEnum.PIPEDELIMITED.equals(queryParameter.getStyle())) {
                            paramPart.append("|");
                        }
                        else if (Parameter.StyleEnum.SPACEDELIMITED.equals(queryParameter.getStyle())) {
                            paramPart.append("%20");
                        }
                    } else {
                        paramPart.append(param.getName());
                    }

                    paramPart.append("}");
                    if (!param.getRequired()) {
                        paramPart.append("]");
                    }
                    sb.append(paramPart.toString());
                }
            }
        }

        return sb.toString();
    }

    /**
     * Return the instantiation type of the property, especially for map and array
     *
     * @param property Swagger property object
     * @return string presentation of the instantiation type of the property
     */
    public String toInstantiationType(Schema property) {
        if (property instanceof MapSchema || property.getAdditionalProperties() != null && property.getAdditionalProperties() instanceof Schema) {
            Schema additionalProperties = (Schema) property.getAdditionalProperties();
            String type = additionalProperties.getType();
            if (null == type) {
                LOGGER.error("No Type defined for Additional Property " + additionalProperties + "\n" //
                        + "\tIn Property: " + property);
            }
            String inner = getSchemaType(additionalProperties);
            return instantiationTypes.get("map") + "<String, " + inner + ">";
        } else if (property instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) property;
            String inner = getSchemaType(arraySchema.getItems());
            return instantiationTypes.get("array") + "<" + inner + ">";
        } else {
            return null;
        }
    }

    /**
     * Return the example value of the parameter.
     *
     * @param p Swagger property object
     */
    public void setParameterExampleValue(CodegenParameter p) {

    }

    /**
     * Return the example value of the property
     *
     * @param property Schema property object
     * @return string presentation of the example value of the property
     */
    public String toExampleValue(Schema property) {
        return String.valueOf(property.getExample());
    }

    /**
     * Return the default value of the property
     *
     * @param property Schema property object
     * @return string presentation of the default value of the property
     */
    @SuppressWarnings("static-method")
    public String toDefaultValue(Schema property) {
        return String.valueOf(property.getDefault());
    }

    /**
     * Return the property initialized from a data object
     * Useful for initialization with a plain object in Javascript
     *
     * @param name Name of the property object
     * @param property openAPI schema object
     * @return string presentation of the default value of the property
     */
    @SuppressWarnings("static-method")
    public String toDefaultValueWithParam(String name, Schema property) {
        return " = data." + name + ";";
    }

    /**
     * returns the swagger type for the property
     * @param property Schema property object
     * @return string presentation of the type
     **/
    @SuppressWarnings("static-method")
    public String getSchemaType(Schema property) {
        String datatype = null;

        if (StringUtils.isNotBlank(property.get$ref())) {
            try {
                datatype = property.get$ref();
                if (datatype.indexOf("#/components/schemas/") == 0) {
                    datatype = datatype.substring("#/components/schemas/".length());
                    return datatype;
                }
            } catch (Exception e) {
                LOGGER.warn("Error obtaining the datatype from ref:" + property + ". Datatype default to Object");
                datatype = "Object";
                LOGGER.error(e.getMessage(), e);
            }
            return datatype;
        }

        if (property instanceof StringSchema && "number".equals(property.getFormat())) {
            datatype = "BigDecimal";
        } else if (property instanceof ByteArraySchema) {
            datatype = "ByteArray";
        } else if (property instanceof BinarySchema) {
            datatype = SchemaTypeUtil.BINARY_FORMAT;
        } else if (property instanceof FileSchema) {
            datatype = "file";
        } else if (property instanceof BooleanSchema) {
            datatype = SchemaTypeUtil.BOOLEAN_TYPE;
        } else if (property instanceof DateSchema) {
            datatype = SchemaTypeUtil.DATE_FORMAT;
        } else if (property instanceof DateTimeSchema) {
            datatype = "DateTime";
        } else if (property instanceof NumberSchema) {
            if(SchemaTypeUtil.FLOAT_FORMAT.equals(property.getFormat())) {
                datatype = SchemaTypeUtil.FLOAT_FORMAT;
            } else if(SchemaTypeUtil.DOUBLE_FORMAT.equals(property.getFormat())) {
                datatype = SchemaTypeUtil.DOUBLE_FORMAT;
            } else {
                datatype = "BigDecimal";
            }
        } else if (property instanceof IntegerSchema) {
            if(SchemaTypeUtil.INTEGER64_FORMAT.equals(property.getFormat())) {
                datatype = "long";
            } else {
                datatype = property.getType();
            }
        } else if (property instanceof MapSchema) {
            datatype = "map";
        } else if ( property instanceof UUIDSchema) {
            datatype = "UUID";
        } else if (property instanceof StringSchema) {
            datatype = "string";
        } else {
            if (property != null) {
                if (SchemaTypeUtil.OBJECT_TYPE.equals(property.getType()) && property.getAdditionalProperties() != null) {
                    datatype = "map";
                } else {
                    datatype = property.getType();
                }
            }
        }
        return datatype;
    }

    /**
     * Return the snake-case of the string
     *
     * @param name string to be snake-cased
     * @return snake-cased string
     */
    @SuppressWarnings("static-method")
    public String snakeCase(String name) {
        return (name.length() > 0) ? (Character.toLowerCase(name.charAt(0)) + name.substring(1)) : "";
    }

    /**
     * Capitalize the string
     *
     * @param name string to be capitalized
     * @return capitalized string
     */
    @SuppressWarnings("static-method")
    public String initialCaps(String name) {
        return StringUtils.capitalize(name);
    }

    /**
     * Output the type declaration of a given name
     *
     * @param name name
     * @return a string presentation of the type
     */
    @SuppressWarnings("static-method")
    public String getTypeDeclaration(String name) {
        return name;
    }

    /**
     * Output the type declaration of the property
     *
     * @param schema Schema Property object
     * @return a string presentation of the property type
     */
    public String getTypeDeclaration(Schema schema) {
        String swaggerType = getSchemaType(schema);
        if (typeMapping.containsKey(swaggerType)) {
            return typeMapping.get(swaggerType);
        }
        return swaggerType;
    }

    /**
     * Determine the type alias for the given type if it exists. This feature
     * is only used for Java, because the language does not have a aliasing
     * mechanism of its own.
     * @param name The type name.
     * @return The alias of the given type, if it exists. If there is no alias
     * for this type, then returns the input type name.
     */
    public String getAlias(String name) {
        return name;
    }

    /**
     * Output the Getter name for boolean property, e.g. getActive
     *
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    public String toBooleanGetter(String name) {
        return "get" + getterAndSetterCapitalize(name);
    }

    /**
     * Output the Getter name, e.g. getSize
     *
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    public String toGetter(String name) {
        return "get" + getterAndSetterCapitalize(name);
    }

    /**
     * Output the Getter name, e.g. getSize
     *
     * @param name the name of the property
     * @return setter name based on naming convention
     */
    public String toSetter(String name) {
        return "set" + getterAndSetterCapitalize(name);
    }

    /**
     * Output the API (class) name (capitalized) ending with "Api"
     * Return DefaultApi if name is empty
     *
     * @param name the name of the Api
     * @return capitalized Api name ending with "Api"
     */
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        return initialCaps(name) + "Api";
    }

    /**
     * Output the proper model name (capitalized).
     * In case the name belongs to the TypeSystem it won't be renamed.
     *
     * @param name the name of the model
     * @return capitalized model name
     */
    public String toModelName(final String name) {
        return initialCaps(modelNamePrefix + name + modelNameSuffix);
    }

    /**
     * Convert Swagger Model object to Codegen Model object without providing all model definitions
     *
     * @param name the name of the model
     * @param schema Schema object
     * @return Codegen Model object
     */
    public CodegenModel fromModel(String name, Schema schema) {
        return fromModel(name, schema, null);
    }

    /**
     * Convert Swagger Model object to Codegen Model object
     *
     * @param name the name of the model
     * @param schema Swagger Model object
     * @param allDefinitions a map of all Swagger models from the spec
     * @return Codegen Model object
     */
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allDefinitions) {
        if (typeAliases == null) {
            // Only do this once during first call
            typeAliases = getAllAliases(allDefinitions);
        }
        final CodegenModel codegenModel = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
        if (reservedWords.contains(name)) {
            codegenModel.name = escapeReservedWord(name);
        } else {
            codegenModel.name = name;
        }
        codegenModel.title = escapeText(schema.getTitle());
        codegenModel.description = escapeText(schema.getDescription());
        codegenModel.unescapedDescription = schema.getDescription();
        codegenModel.classname = toModelName(name);
        codegenModel.classVarName = toVarName(name);
        codegenModel.classFilename = toModelFilename(name);
        codegenModel.modelJson = Json.pretty(schema);
        codegenModel.externalDocumentation = schema.getExternalDocs();
        if (schema.getExtensions() != null && !schema.getExtensions().isEmpty()) {
            codegenModel.getVendorExtensions().putAll(schema.getExtensions());
        }
        codegenModel.getVendorExtensions().put(CodegenConstants.IS_ALIAS_EXT_NAME, typeAliases.containsKey(name));

        codegenModel.discriminator = schema.getDiscriminator();

        if (schema.getXml() != null) {
            codegenModel.xmlPrefix = schema.getXml().getPrefix();
            codegenModel.xmlNamespace = schema.getXml().getNamespace();
            codegenModel.xmlName = schema.getXml().getName();
        }

        if (schema instanceof ArraySchema) {
            codegenModel.getVendorExtensions().put(IS_ARRAY_MODEL_EXT_NAME, Boolean.TRUE);
            codegenModel.arrayModelType = fromProperty(name, schema).complexType;
            addParentContainer(codegenModel, name, schema);
            //} else if (schema instanceof RefModel) {
            // TODO
        } else if (schema instanceof ComposedSchema) {
            final ComposedSchema composed = (ComposedSchema) schema;
            Map<String, Schema> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<String>();
            Map<String, Schema> allProperties;
            List<String> allRequired;
            if (supportsInheritance || supportsMixins) {
                allProperties = new LinkedHashMap<>();
                allRequired = new ArrayList<String>();
                codegenModel.allVars = new ArrayList<CodegenProperty>();
                int modelImplCnt = 0; // only one inline object allowed in a ComposedModel
                for (Schema innerModel: composed.getAllOf()) {
                    if (codegenModel.discriminator == null) {
                        codegenModel.discriminator = schema.getDiscriminator();
                    }
                    if (innerModel.getXml() != null) {
                        codegenModel.xmlPrefix = innerModel.getXml().getPrefix();
                        codegenModel.xmlNamespace = innerModel.getXml().getNamespace();
                        codegenModel.xmlName = innerModel.getXml().getName();
                    }
                    if (modelImplCnt++ > 1) {
                        LOGGER.warn("More than one inline schema specified in allOf:. Only the first one is recognized. All others are ignored.");
                        break; // only one ModelImpl with discriminator allowed in allOf
                    }
                }
            } else {
                allProperties = null;
                allRequired = null;
            }
            // parent model
            final String parentName = getParentName(composed, allDefinitions);
            final Schema parent = StringUtils.isBlank(parentName) ? null : allDefinitions.get(parentName);

            List<Schema> interfaces = getInterfaces(composed);

            // interfaces (intermediate models)
            if (interfaces != null) {
                if (codegenModel.interfaces == null) {
                    codegenModel.interfaces = new ArrayList<String>();
                }
                for (Schema interfaceSchema : interfaces) {
                    if (StringUtils.isBlank(interfaceSchema.get$ref())) {
                        continue;
                    }
                    Schema refSchema = null;
                    String ref = getSimpleRef(interfaceSchema.get$ref());
                    if (allDefinitions != null) {
                        refSchema = allDefinitions.get(ref);
                    }
                    final String modelName = toModelName(ref);
                    codegenModel.interfaces.add(modelName);
                    addImport(codegenModel, modelName);
                    if (allDefinitions != null && refSchema != null) {
                        if (!supportsMixins) {
                            addProperties(properties, required, refSchema, allDefinitions);
                        }
                        if (supportsInheritance) {
                            addProperties(allProperties, allRequired, refSchema, allDefinitions);
                        }
                    }
                }
            }
            if (parent != null) {
                codegenModel.parentSchema = parentName;
                codegenModel.parent = StringUtils.capitalize(modelNamePrefix + parentName + modelNameSuffix);
                addImport(codegenModel, codegenModel.parent);
                if (allDefinitions != null) {
                    if (supportsInheritance) {
                        addProperties(allProperties, allRequired, parent, allDefinitions);
                    } else {
                        addProperties(properties, required, parent, allDefinitions);
                    }
                }
            }
            addProperties(properties, required, composed, allDefinitions);
            addVars(codegenModel, properties, required, allProperties, allRequired);
        } else {
            codegenModel.dataType = getSchemaType(schema);
            if(schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                codegenModel.getVendorExtensions().put(CodegenConstants.IS_ENUM_EXT_NAME, Boolean.TRUE);
                // comment out below as allowableValues is not set in post processing model enum
                codegenModel.allowableValues = new HashMap<String, Object>();
                codegenModel.allowableValues.put("values", schema.getEnum());
            }
            if (schema.getAdditionalProperties() != null) {
                addAdditionPropertiesToCodeGenModel(codegenModel, schema);
            }
            addVars(codegenModel, schema.getProperties(), schema.getRequired());
        }

        if (codegenModel.vars != null) {
            for(CodegenProperty prop : codegenModel.vars) {
                postProcessModelProperty(codegenModel, prop);
            }
        }
        return codegenModel;
    }

    /**
     * Recursively look for a discriminator in the interface tree
     */
    private boolean isDiscriminatorInInterfaceTree(ComposedSchema composedSchema, Map<String, Schema> allSchema) {
        if (composedSchema == null || allSchema == null || allSchema.isEmpty()) {
            return false;
        }
        if (composedSchema.getDiscriminator() != null) {
            return true;
        }
        final List<Schema> interfaces = getInterfaces(composedSchema);
        if(interfaces == null) {
            return false;
        }
        for (Schema interfaceSchema : interfaces) {
            if (interfaceSchema.getDiscriminator() != null) {
                return true;
            }
        }
        return false;
    }

    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        addParentContainer(codegenModel, codegenModel.name, schema);
    }

    protected void addProperties(Map<String, Schema> properties, List<String> required, Schema schema, Map<String, Schema> allSchemas) {
        if(schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            if(composedSchema.getAllOf() == null) {
                return;
            }
            for (Schema component : composedSchema.getAllOf()) {
                addProperties(properties, required, component, allSchemas);
            }
            return;
        }
        if(StringUtils.isNotBlank(schema.get$ref())) {
            Schema interfaceSchema = allSchemas.get(getSimpleRef(schema.get$ref()));
            addProperties(properties, required, interfaceSchema, allSchemas);
            return;
        }
        if(schema.getProperties() != null) {
            properties.putAll(schema.getProperties());
        }
        if(schema.getRequired() != null) {
            required.addAll(schema.getRequired());
        }
    }

    /**
     * Camelize the method name of the getter and setter
     *
     * @param name string to be camelized
     * @return Camelized string
     */
    public String getterAndSetterCapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return camelize(toVarName(name));
    }

    /**
     * Convert Swagger Property object to Codegen Property object
     *
     * @param name name of the property
     * @param propertySchema Schema object
     * @return Codegen Property object
     * TODO : improve repeated code
     */
    public CodegenProperty fromProperty(String name, Schema propertySchema) {
        if (propertySchema == null) {
            LOGGER.error("unexpected missing property for name " + name);
            return null;
        }

        final CodegenProperty codegenProperty = CodegenModelFactory.newInstance(CodegenModelType.PROPERTY);
        codegenProperty.name = toVarName(name);
        codegenProperty.baseName = name;
        codegenProperty.nameInCamelCase = camelize(codegenProperty.name, false);
        codegenProperty.description = escapeText(propertySchema.getDescription());
        codegenProperty.unescapedDescription = propertySchema.getDescription();
        codegenProperty.title = propertySchema.getTitle();
        codegenProperty.getter = toGetter(name);
        codegenProperty.setter = toSetter(name);
        String example = toExampleValue(propertySchema);
        if(!"null".equals(example)) {
            codegenProperty.example = example;
        }
        codegenProperty.defaultValue = toDefaultValue(propertySchema);
        codegenProperty.defaultValueWithParam = toDefaultValueWithParam(name, propertySchema);
        codegenProperty.jsonSchema = Json.pretty(propertySchema);
        if (propertySchema.getReadOnly() != null) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_READ_ONLY_EXT_NAME, propertySchema.getReadOnly());
        }
        if (propertySchema.getXml() != null) {
            if (propertySchema.getXml().getAttribute() != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_XML_ATTRIBUTE_EXT_NAME, propertySchema.getXml().getAttribute());
            }
            codegenProperty.xmlPrefix = propertySchema.getXml().getPrefix();
            codegenProperty.xmlName = propertySchema.getXml().getName();
            codegenProperty.xmlNamespace = propertySchema.getXml().getNamespace();
        }
        if (propertySchema.getExtensions() != null && !propertySchema.getExtensions().isEmpty()) {
            codegenProperty.getVendorExtensions().putAll(propertySchema.getExtensions());
        }

        final String type = getSchemaType(propertySchema);
        if (propertySchema instanceof IntegerSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            if(SchemaTypeUtil.INTEGER64_FORMAT.equals(propertySchema.getFormat())) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_LONG_EXT_NAME, Boolean.TRUE);
            } else {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_INTEGER_EXT_NAME, Boolean.TRUE);
            }
            if (propertySchema.getMinimum() != null) {
                codegenProperty.minimum = String.valueOf(propertySchema.getMinimum().longValue());
            }
            if (propertySchema.getMaximum() != null) {
                codegenProperty.maximum = String.valueOf(propertySchema.getMaximum().longValue());
            }
            if (propertySchema.getExclusiveMinimum() != null) {
                codegenProperty.exclusiveMinimum = propertySchema.getExclusiveMinimum();
            }
            if (propertySchema.getExclusiveMaximum() != null) {
                codegenProperty.exclusiveMaximum = propertySchema.getExclusiveMaximum();
            }

            // check if any validation rule defined
            // exclusive* are noop without corresponding min/max
            if (codegenProperty.minimum != null || codegenProperty.maximum != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_VALIDATION_EXT_NAME, Boolean.TRUE);
            }

            // legacy support
            Map<String, Object> allowableValues = new HashMap<String, Object>();
            if (propertySchema.getMinimum() != null) {
                allowableValues.put("min", propertySchema.getMinimum());
            }
            if (propertySchema.getMaximum() != null) {
                allowableValues.put("max", propertySchema.getMaximum());
            }
            if (propertySchema.getEnum() != null) {
                List<Integer> _enum = propertySchema.getEnum();
                codegenProperty._enum = new ArrayList<String>();
                for(Integer i : _enum) {
                    codegenProperty._enum.add(i.toString());
                }
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);
                allowableValues.put("values", _enum);
            }
            if(allowableValues.size() > 0) {
                codegenProperty.allowableValues = allowableValues;
            }
        }

        if (propertySchema instanceof StringSchema) {
            codegenProperty.maxLength = propertySchema.getMaxLength();
            codegenProperty.minLength = propertySchema.getMinLength();
            codegenProperty.pattern = toRegularExpression(propertySchema.getPattern());

            // check if any validation rule defined
            if (codegenProperty.pattern != null || codegenProperty.minLength != null || codegenProperty.maxLength != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_VALIDATION_EXT_NAME, Boolean.TRUE);
            }

            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
            if (propertySchema.getEnum() != null) {
                List<String> _enum = propertySchema.getEnum();
                codegenProperty._enum = _enum;
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

                // legacy support
                Map<String, Object> allowableValues = new HashMap<String, Object>();
                allowableValues.put("values", _enum);
                codegenProperty.allowableValues = allowableValues;
            }
        }
        if (propertySchema instanceof BooleanSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_BOOLEAN_EXT_NAME, Boolean.TRUE);
            codegenProperty.getter = toBooleanGetter(name);
        }
        if (propertySchema instanceof BinarySchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof FileSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof EmailSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof UUIDSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_UUID_EXT_NAME, Boolean.TRUE);
            // keep isString to true to make it backward compatible
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        }
        if (propertySchema instanceof ByteArraySchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
        }
        // type is number and without format
        if (propertySchema instanceof NumberSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            if(SchemaTypeUtil.FLOAT_FORMAT.equals(propertySchema.getFormat())) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_FLOAT_EXT_NAME, Boolean.TRUE);
            } else {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_DOUBLE_EXT_NAME, Boolean.TRUE);
            }
            if (propertySchema.getEnum() != null) {
                List<Double> _enum = propertySchema.getEnum();
                codegenProperty._enum = new ArrayList<String>();
                for(Double i : _enum) {
                    codegenProperty._enum.add(i.toString());
                }
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

                // legacy support
                Map<String, Object> allowableValues = new HashMap<String, Object>();
                allowableValues.put("values", _enum);
                codegenProperty.allowableValues = allowableValues;
            }
        }
        if (propertySchema instanceof DateSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_DATE_EXT_NAME, Boolean.TRUE);
            if (propertySchema.getEnum() != null) {
                List<String> _enum = propertySchema.getEnum();
                codegenProperty._enum = new ArrayList<String>();
                for(String i : _enum) {
                    codegenProperty._enum.add(i);
                }
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

                // legacy support
                Map<String, Object> allowableValues = new HashMap<String, Object>();
                allowableValues.put("values", _enum);
                codegenProperty.allowableValues = allowableValues;
            }
        }
        if (propertySchema instanceof DateTimeSchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_DATE_TIME_EXT_NAME, Boolean.TRUE);
            if (propertySchema.getEnum() != null) {
                List<String> _enum = propertySchema.getEnum();
                codegenProperty._enum = new ArrayList<String>();
                for(String i : _enum) {
                    codegenProperty._enum.add(i);
                }
                codegenProperty.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);

                // legacy support
                Map<String, Object> allowableValues = new HashMap<String, Object>();
                allowableValues.put("values", _enum);
                codegenProperty.allowableValues = allowableValues;
            }
        }
        codegenProperty.datatype = getTypeDeclaration(propertySchema);
        codegenProperty.dataFormat = propertySchema.getFormat();

        // this can cause issues for clients which don't support enums
        boolean isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
        if (isEnum) {
            codegenProperty.datatypeWithEnum = toEnumName(codegenProperty);
            codegenProperty.enumName = toEnumName(codegenProperty);
        } else {
            codegenProperty.datatypeWithEnum = codegenProperty.datatype;
        }

        codegenProperty.baseType = getSchemaType(propertySchema);

        if (propertySchema instanceof ArraySchema) {
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.containerType = "array";
            codegenProperty.baseType = getSchemaType(propertySchema);
            if (propertySchema.getXml() != null) {
                codegenProperty.getVendorExtensions().put(CodegenConstants.IS_XML_WRAPPED_EXT_NAME,
                        propertySchema.getXml().getWrapped() == null ? false : propertySchema.getXml().getWrapped());
                codegenProperty.xmlPrefix= propertySchema.getXml().getPrefix();
                codegenProperty.xmlNamespace = propertySchema.getXml().getNamespace();
                codegenProperty.xmlName = propertySchema.getXml().getName();
            }
            // handle inner property
            codegenProperty.maxItems = propertySchema.getMaxItems();
            codegenProperty.minItems = propertySchema.getMinItems();
            String itemName = null;
            if (propertySchema.getExtensions() != null && propertySchema.getExtensions().get("x-item-name") != null) {
                itemName = propertySchema.getExtensions().get("x-item-name").toString();
            }
            if (itemName == null) {
                itemName = codegenProperty.name;
            }
            Schema items = ((ArraySchema) propertySchema).getItems();
            CodegenProperty innerCodegenProperty = fromProperty(itemName, items);
            updatePropertyForArray(codegenProperty, innerCodegenProperty);
        } else if (propertySchema instanceof MapSchema || (propertySchema.getAdditionalProperties() != null && propertySchema.getAdditionalProperties() instanceof Schema)) {

            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenProperty.containerType = "map";
            codegenProperty.baseType = getSchemaType(propertySchema);
            codegenProperty.minItems = propertySchema.getMinProperties();
            codegenProperty.maxItems = propertySchema.getMaxProperties();

            // handle inner property
            CodegenProperty cp = fromProperty("inner", (Schema) propertySchema.getAdditionalProperties());
            updatePropertyForMap(codegenProperty, cp);
        } else {
            if (StringUtils.isNotBlank(propertySchema.get$ref())) {
                codegenProperty.baseType = getSimpleRef(propertySchema.get$ref());
            }
            setNonArrayMapProperty(codegenProperty, type);
        }
        return codegenProperty;
    }

    /**
     * Update property for array(list) container
     * @param property Codegen property
     * @param innerProperty Codegen inner property of map or list
     */
    protected void updatePropertyForArray(CodegenProperty property, CodegenProperty innerProperty) {
        if (innerProperty == null) {
            LOGGER.warn("skipping invalid array property " + Json.pretty(property));
            return;
        }
        property.dataFormat = innerProperty.dataFormat;
        if (!languageSpecificPrimitives.contains(innerProperty.baseType)) {
            property.complexType = innerProperty.baseType;
        } else {
            property.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
        }
        property.items = innerProperty;
        // inner item is Enum
        if (isPropertyInnerMostEnum(property)) {
            // isEnum is set to true when the type is an enum
            // or the inner type of an array/map is an enum
            property.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);
            // update datatypeWithEnum and default value for array
            // e.g. List<string> => List<StatusEnum>
            updateDataTypeWithEnumForArray(property);
            // set allowable values to enum values (including array/map of enum)
            property.allowableValues = getInnerEnumAllowableValues(property);
        }

    }

    /**
     * Update property for map container
     * @param property Codegen property
     * @param innerProperty Codegen inner property of map or list
     */
    protected void updatePropertyForMap(CodegenProperty property, CodegenProperty innerProperty) {
        if (innerProperty == null) {
            LOGGER.warn("skipping invalid map property " + Json.pretty(property));
            return;
        }
        if (!languageSpecificPrimitives.contains(innerProperty.baseType)) {
            property.complexType = innerProperty.baseType;
        } else {
            property.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
        }
        property.items = innerProperty;
        property.dataFormat = innerProperty.dataFormat;
        // inner item is Enum
        if (isPropertyInnerMostEnum(property)) {
            // isEnum is set to true when the type is an enum
            // or the inner type of an array/map is an enum
            property.getVendorExtensions().put(IS_ENUM_EXT_NAME, Boolean.TRUE);
            // update datatypeWithEnum and default value for map
            // e.g. Dictionary<string, string> => Dictionary<string, StatusEnum>
            updateDataTypeWithEnumForMap(property);
            // set allowable values to enum values (including array/map of enum)
            property.allowableValues = getInnerEnumAllowableValues(property);
        }

    }

    /**
     * Update property for map container
     * @param property Codegen property
     * @return True if the inner most type is enum
     */
    protected Boolean isPropertyInnerMostEnum(CodegenProperty property) {
        CodegenProperty currentProperty = property;
        boolean isMapContainer = getBooleanValue(property, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME);
        boolean isListContainer = getBooleanValue(property, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME);
        while (currentProperty != null && (isMapContainer || isListContainer)) {
            currentProperty = currentProperty.items;
        }
        boolean isEnum = getBooleanValue(currentProperty, IS_ENUM_EXT_NAME);
        return currentProperty == null ? false : isEnum;
    }

    protected Map<String, Object> getInnerEnumAllowableValues(CodegenProperty property) {
        CodegenProperty currentProperty = property;
        boolean isMapContainer = getBooleanValue(property, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME);
        boolean isListContainer = getBooleanValue(property, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME);
        while (currentProperty != null && (isMapContainer || isListContainer)) {
            currentProperty = currentProperty.items;
        }

        return currentProperty == null ? new HashMap<String, Object>() : currentProperty.allowableValues;
    }


    /**
     * Update datatypeWithEnum for array container
     * @param property Codegen property
     */
    protected void updateDataTypeWithEnumForArray(CodegenProperty property) {
        CodegenProperty baseItem = property.items;
        boolean isMapContainer = getBooleanValue(baseItem, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME);
        boolean isListContainer = getBooleanValue(baseItem, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME);
        while (baseItem != null && (isMapContainer || isListContainer)) {
            baseItem = baseItem.items;
        }
        if (baseItem != null) {
            // set both datatype and datetypeWithEnum as only the inner type is enum
            property.datatypeWithEnum = property.datatypeWithEnum.replace(baseItem.baseType, toEnumName(baseItem));

            // naming the enum with respect to the language enum naming convention
            // e.g. remove [], {} from array/map of enum
            property.enumName = toEnumName(property);

            // set default value for variable with inner enum
            if (property.defaultValue != null) {
                property.defaultValue = property.defaultValue.replace(baseItem.baseType, toEnumName(baseItem));
            }

            updateCodegenPropertyEnum(property);
        }
    }

    /**
     * Update datatypeWithEnum for map container
     * @param property Codegen property
     */
    protected void updateDataTypeWithEnumForMap(CodegenProperty property) {
        CodegenProperty baseItem = property.items;
        boolean isMapContainer = getBooleanValue(baseItem, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME);
        boolean isListContainer = getBooleanValue(baseItem, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME);
        while (baseItem != null && (isMapContainer || isListContainer)) {
            baseItem = baseItem.items;
        }

        if (baseItem != null) {
            // set both datatype and datetypeWithEnum as only the inner type is enum
            property.datatypeWithEnum = property.datatypeWithEnum.replace(", " + baseItem.baseType, ", " + toEnumName(baseItem));

            // naming the enum with respect to the language enum naming convention
            // e.g. remove [], {} from array/map of enum
            property.enumName = toEnumName(property);

            // set default value for variable with inner enum
            if (property.defaultValue != null) {
                property.defaultValue = property.defaultValue.replace(", " + property.items.baseType, ", " + toEnumName(property.items));
            }
        }
    }

    protected void setNonArrayMapProperty(CodegenProperty property, String type) {
        property.getVendorExtensions().put(CodegenConstants.IS_NOT_CONTAINER_EXT_NAME, Boolean.TRUE);
        if (languageSpecificPrimitives().contains(type)) {
            property.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
        } else {
            property.complexType = property.baseType;
        }
    }

    /**
     * Override with any special handling of response codes
     * @param responses Swagger Operation's responses
     * @return default method response or <tt>null</tt> if not found
     */
    protected ApiResponse findMethodResponse(ApiResponses responses) {

        String code = null;
        for (String responseCode : responses.keySet()) {
            if (responseCode.startsWith("2") || responseCode.equals("default")) {
                if (code == null || code.compareTo(responseCode) > 0) {
                    code = responseCode;
                }
            }
        }
        if (code == null) {
            return null;
        }
        return responses.get(code);
    }

    /**
     * Convert Swagger Operation object to Codegen Operation object (without providing a Swagger object)
     *
     * @param path the path of the operation
     * @param httpMethod HTTP method
     * @param operation Swagger operation object
     * @param schemas a map of Swagger models
     * @return Codegen Operation object
     */
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Schema> schemas) {
        return fromOperation(path, httpMethod, operation, schemas, null);
    }

    /**
     * Convert Swagger Operation object to Codegen Operation object
     *
     * @param path the path of the operation
     * @param httpMethod HTTP method
     * @param operation Swagger operation object
     * @param schemas a map of schemas
     * @param openAPI a OpenAPI object representing the spec
     * @return Codegen Operation object
     */
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Schema> schemas, OpenAPI openAPI) {
        CodegenOperation codegenOperation = CodegenModelFactory.newInstance(CodegenModelType.OPERATION);
        Set<String> imports = new HashSet<String>();
        if (operation.getExtensions() != null && !operation.getExtensions().isEmpty()) {
            codegenOperation.vendorExtensions.putAll(operation.getExtensions());
        }

        String operationId = getOrGenerateOperationId(operation, path, httpMethod);
        // remove prefix in operationId
        if (removeOperationIdPrefix) {
            int offset = operationId.indexOf('_');
            if (offset > -1) {
                operationId = operationId.substring(offset+1);
            }
        }
        operationId = removeNonNameElementToCamelCase(operationId);
        codegenOperation.path = path;
        codegenOperation.operationId = toOperationId(operationId);
        codegenOperation.summary = escapeText(operation.getSummary());
        codegenOperation.unescapedNotes = operation.getDescription();
        codegenOperation.notes = escapeText(operation.getDescription());
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_CONSUMES_EXT_NAME, Boolean.FALSE);
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_PRODUCES_EXT_NAME, Boolean.FALSE);
        if (operation.getDeprecated() != null) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_DEPRECATED_EXT_NAME, operation.getDeprecated());
        }

        addConsumesInfo(operation, codegenOperation);

        if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            ApiResponse methodResponse = findMethodResponse(operation.getResponses());

            for (String key : operation.getResponses().keySet()) {
                ApiResponse response = operation.getResponses().get(key);

                addProducesInfo(response, codegenOperation);

                CodegenResponse codegenResponse = fromResponse(key, response);
                codegenResponse.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.TRUE);
                if (codegenResponse.baseType != null && !defaultIncludes.contains(codegenResponse.baseType) && !languageSpecificPrimitives.contains(codegenResponse.baseType)) {
                    imports.add(codegenResponse.baseType);
                }
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DEFAULT_EXT_NAME, response == methodResponse);
                codegenOperation.responses.add(codegenResponse);
                if (getBooleanValue(codegenResponse, CodegenConstants.IS_BINARY_EXT_NAME) && getBooleanValue(codegenResponse, CodegenConstants.IS_DEFAULT_EXT_NAME)) {
                    codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESPONSE_BINARY_EXT_NAME, Boolean.TRUE);
                }
                if (getBooleanValue(codegenResponse, CodegenConstants.IS_FILE_EXT_NAME) && getBooleanValue(codegenResponse, CodegenConstants.IS_DEFAULT_EXT_NAME)) {
                    codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESPONSE_FILE_EXT_NAME, Boolean.TRUE);
                }
            }
            codegenOperation.responses.get(codegenOperation.responses.size() - 1).getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.FALSE);

            if (methodResponse != null) {
                final Schema responseSchema = getSchemaFromResponse(methodResponse);
                if (responseSchema != null) {
                    final CodegenProperty codegenProperty = fromProperty("response", responseSchema);

                    if (responseSchema instanceof ArraySchema) {
                        ArraySchema arraySchema = (ArraySchema) responseSchema;
                        CodegenProperty innerProperty = fromProperty("response", arraySchema.getItems());
                        codegenOperation.returnBaseType = innerProperty.baseType;
                    } else if (responseSchema instanceof MapSchema) {
                        MapSchema mapSchema = (MapSchema) responseSchema;
                        CodegenProperty innerProperty = fromProperty("response", (Schema) mapSchema.getAdditionalProperties());
                        codegenOperation.returnBaseType = innerProperty.baseType;
                    } else {
                        if (codegenProperty.complexType != null) {
                            codegenOperation.returnBaseType = codegenProperty.complexType;
                        } else {
                            codegenOperation.returnBaseType = codegenProperty.baseType;
                        }
                    }
                    //TODO: codegenOperation.examples = new ExampleGenerator(schemas).generate(methodResponse.getExamples(), operation.getProduces(), responseProperty);
                    codegenOperation.defaultResponse = toDefaultValue(responseSchema);
                    codegenOperation.returnType = codegenProperty.datatype;
                    boolean hasReference = schemas != null && schemas.containsKey(codegenOperation.returnBaseType);
                    codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_REFERENCE_EXT_NAME, hasReference);

                    // lookup discriminator
                    if (schemas != null) {
                        Schema schemaDefinition = schemas.get(codegenOperation.returnBaseType);
                        if (schemaDefinition != null) {
                            CodegenModel cmod = fromModel(codegenOperation.returnBaseType, schemaDefinition, schemas);
                            codegenOperation.discriminator = cmod.discriminator;
                        }
                    }

                    boolean isContainer = getBooleanValue(codegenProperty, CodegenConstants.IS_CONTAINER_EXT_NAME);
                    if (isContainer) {
                        codegenOperation.returnContainer = codegenProperty.containerType;
                        if ("map".equals(codegenProperty.containerType)) {
                            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
                        } else if ("list".equalsIgnoreCase(codegenProperty.containerType)) {
                            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);
                        } else if ("array".equalsIgnoreCase(codegenProperty.containerType)) {
                            codegenOperation.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);
                        }
                    } else {
                        codegenOperation.returnSimpleType = true;
                    }
                    if (languageSpecificPrimitives().contains(codegenOperation.returnBaseType) || codegenOperation.returnBaseType == null) {
                        codegenOperation.returnTypeIsPrimitive = true;
                    }
                }
                addHeaders(methodResponse, codegenOperation.responseHeaders);
            }
        }

        List<Parameter> parameters = operation.getParameters();
        CodegenParameter bodyParam = null;
        List<CodegenParameter> allParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> bodyParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> pathParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> queryParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> headerParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> cookieParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> formParams = new ArrayList<CodegenParameter>();
        List<CodegenParameter> requiredParams = new ArrayList<CodegenParameter>();

        if (operation.getRequestBody() != null) {
            bodyParam = fromRequestBody(operation.getRequestBody(), schemas, imports);
            bodyParams.add(bodyParam);
            allParams.add(bodyParam);
        }

        if (parameters != null) {
            for (Parameter param : parameters) {
                if (StringUtils.isNotBlank(param.get$ref())) {
                    param = getParameterFromRef(param.get$ref(), openAPI);
                }
                CodegenParameter codegenParameter = fromParameter(param, imports);
                // rename parameters to make sure all of them have unique names
                if (ensureUniqueParams) {
                    while (true) {
                        boolean exists = false;
                        for (CodegenParameter cp : allParams) {
                            if (codegenParameter.paramName.equals(cp.paramName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            codegenParameter.paramName = generateNextName(codegenParameter.paramName);
                        } else {
                            break;
                        }
                    }
                }

                // set isPrimitiveType and baseType for allParams
                /*if (languageSpecificPrimitives.contains(p.baseType)) {
                    p.isPrimitiveType = true;
                    p.baseType = getSwaggerType(p);
                }*/


                allParams.add(codegenParameter);
                // Issue #2561 (neilotoole) : Moved setting of is<Type>Param flags
                // from here to fromParameter().
                if (param instanceof QueryParameter) {
                    queryParams.add(codegenParameter.copy());
                } else if (param instanceof PathParameter) {
                    pathParams.add(codegenParameter.copy());
                } else if (param instanceof HeaderParameter) {
                    headerParams.add(codegenParameter.copy());
                } else if (param instanceof CookieParameter) {
                    cookieParams.add(codegenParameter.copy());
                }
                if (!codegenParameter.required) {
                    codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_OPTIONAL_PARAMS_EXT_NAME, Boolean.TRUE);
                } else {
                    requiredParams.add(codegenParameter.copy());
                }
            }
        }

        for (String i : imports) {
            if (needToImport(i)) {
                codegenOperation.imports.add(i);
            }
        }

        codegenOperation.bodyParam = bodyParam;
        codegenOperation.httpMethod = httpMethod.toUpperCase();

        // move "required" parameters in front of "optional" parameters
        if (sortParamsByRequiredFlag) {
            Collections.sort(allParams, new Comparator<CodegenParameter>() {
                @Override
                public int compare(CodegenParameter one, CodegenParameter another) {
                    if (one.required == another.required) return 0;
                    else if (one.required) return -1;
                    else return 1;
                }
            });
        }

        codegenOperation.allParams = addHasMore(allParams);
        codegenOperation.bodyParams = addHasMore(bodyParams);
        codegenOperation.pathParams = addHasMore(pathParams);
        codegenOperation.queryParams = addHasMore(queryParams);
        codegenOperation.headerParams = addHasMore(headerParams);
        // op.cookieParams = cookieParams;
        codegenOperation.formParams = addHasMore(formParams);
        codegenOperation.requiredParams = addHasMore(requiredParams);
        codegenOperation.externalDocs = operation.getExternalDocs();
        // legacy support
        codegenOperation.nickname = codegenOperation.operationId;

        if (codegenOperation.allParams.size() > 0) {
            codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_PARAMS_EXT_NAME, Boolean.TRUE);
        }
        boolean hasRequiredParams = codegenOperation.requiredParams.size() > 0;
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_REQUIRED_PARAMS_EXT_NAME, hasRequiredParams);

        // set Restful Flag
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_SHOW_EXT_NAME, codegenOperation.isRestfulShow());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_INDEX_EXT_NAME, codegenOperation.isRestfulIndex());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_CREATE_EXT_NAME, codegenOperation.isRestfulCreate());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_UPDATE_EXT_NAME, codegenOperation.isRestfulUpdate());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_DESTROY_EXT_NAME, codegenOperation.isRestfulDestroy());
        codegenOperation.getVendorExtensions().put(CodegenConstants.IS_RESTFUL_EXT_NAME, codegenOperation.isRestful());

        return codegenOperation;
    }

    /**
     * Convert Swagger Response object to Codegen Response object
     *
     * @param responseCode HTTP response code
     * @param response Swagger Response object
     * @return Codegen Response object
     */
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        final CodegenResponse codegenResponse = CodegenModelFactory.newInstance(CodegenModelType.RESPONSE);
        if ("default".equals(responseCode)) {
            codegenResponse.code = "0";
        } else {
            codegenResponse.code = responseCode;
        }
        final Schema responseSchema = getSchemaFromResponse(response);
        codegenResponse.schema = responseSchema;
        codegenResponse.message = escapeText(response.getDescription());
        // TODO: codegenResponse.examples = toExamples(response.getExamples());
        codegenResponse.jsonSchema = Json.pretty(response);
        if (response.getExtensions() != null && !response.getExtensions().isEmpty()) {
            codegenResponse.vendorExtensions.putAll(response.getExtensions());
        }
        addHeaders(response, codegenResponse.headers);
        codegenResponse.getVendorExtensions().put(CodegenConstants.HAS_HEADERS_EXT_NAME, !codegenResponse.headers.isEmpty());

        if (responseSchema != null) {
            CodegenProperty codegenProperty = fromProperty("response", responseSchema);

            if (responseSchema instanceof ArraySchema) {
                ArraySchema arraySchema = (ArraySchema) responseSchema;
                CodegenProperty innerProperty = fromProperty("response", arraySchema.getItems());
                codegenResponse.baseType = innerProperty.baseType;
            } else {
                if (codegenProperty.complexType != null) {
                    codegenResponse.baseType = codegenProperty.complexType;
                } else {
                    codegenResponse.baseType = codegenProperty.baseType;
                }
            }
            codegenResponse.dataType = codegenProperty.datatype;

            if (getBooleanValue(codegenProperty, CodegenConstants.IS_STRING_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_BOOLEAN_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_BOOLEAN_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_LONG_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_LONG_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_INTEGER_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_INTEGER_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_DOUBLE_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DOUBLE_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_FLOAT_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_FLOAT_EXT_NAME, Boolean.TRUE);
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_NUMERIC_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_BYTE_ARRAY_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_BINARY_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_FILE_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_DATE_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DATE_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_DATE_TIME_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_DATE_TIME_EXT_NAME, Boolean.TRUE);
            } else if (getBooleanValue(codegenProperty, CodegenConstants.IS_UUID_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_UUID_EXT_NAME, Boolean.TRUE);
            } else {
                LOGGER.debug("Property type is not primitive: " + codegenProperty.datatype);
            }

            if (getBooleanValue(codegenProperty, CodegenConstants.IS_CONTAINER_EXT_NAME)) {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_SIMPLE_TYPE_EXT_NAME, Boolean.FALSE);
                codegenResponse.containerType = codegenProperty.containerType;
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, "map".equals(codegenProperty.containerType));
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME,
                        "list".equalsIgnoreCase(codegenProperty.containerType) || "array".equalsIgnoreCase(codegenProperty.containerType));
            } else {
                codegenResponse.getVendorExtensions().put(CodegenConstants.IS_SIMPLE_TYPE_EXT_NAME, Boolean.TRUE);
            }
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME,
                    (codegenResponse.baseType == null || languageSpecificPrimitives().contains(codegenResponse.baseType)));
        }
        if (codegenResponse.baseType == null) {
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.FALSE);
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.FALSE);
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);
            codegenResponse.getVendorExtensions().put(CodegenConstants.IS_SIMPLE_TYPE_EXT_NAME, Boolean.TRUE);
        }
        return codegenResponse;
    }

    /**
     * Convert Swagger Parameter object to Codegen Parameter object
     *
     * @param parameter Swagger parameter object
     * @param imports set of imports for library/package/module
     * @return Codegen Parameter object
     */
    public CodegenParameter fromParameter(Parameter parameter, Set<String> imports) {
        CodegenParameter codegenParameter = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
        codegenParameter.baseName = parameter.getName();
        codegenParameter.description = escapeText(parameter.getDescription());
        codegenParameter.unescapedDescription = parameter.getDescription();
        if (parameter.getRequired() != null) {
            codegenParameter.required = parameter.getRequired();
        }
        codegenParameter.jsonSchema = Json.pretty(parameter);

        if (System.getProperty("debugParser") != null) {
            LOGGER.info("working on Parameter " + parameter.getName());
        }

        if (parameter.getExtensions() != null && !parameter.getExtensions().isEmpty()) {
            codegenParameter.vendorExtensions.putAll(parameter.getExtensions());
        }

        if (parameter.getSchema() != null) {
            Schema parameterSchema = parameter.getSchema();
            String collectionFormat = null;
            if (parameterSchema instanceof ArraySchema) { // for array parameter
                final ArraySchema arraySchema = (ArraySchema) parameterSchema;
                Schema inner = arraySchema.getItems();
                if (inner == null) {
                    LOGGER.warn("warning!  No inner type supplied for array parameter \"" + parameter.getName() + "\", using String");
                    inner = new StringSchema().description("//TODO automatically added by swagger-codegen");
                    arraySchema.setItems(inner);

                }

                collectionFormat = getCollectionFormat(parameter);

                CodegenProperty codegenProperty = fromProperty("inner", inner);
                codegenParameter.items = codegenProperty;
                codegenParameter.baseType = codegenProperty.datatype;
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);

                // recursively add import
                while (codegenProperty != null) {
                    imports.add(codegenProperty.baseType);
                    codegenProperty = codegenProperty.items;
                }
            } else if (parameterSchema instanceof MapSchema) { // for map parameter
                CodegenProperty codegenProperty = fromProperty("inner", (Schema) parameterSchema.getAdditionalProperties());
                codegenParameter.items = codegenProperty;
                codegenParameter.baseType = codegenProperty.datatype;
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_MAP_CONTAINER_EXT_NAME, Boolean.TRUE);
                // recursively add import
                while (codegenProperty != null) {
                    imports.add(codegenProperty.baseType);
                    codegenProperty = codegenProperty.items;
                }
                collectionFormat = getCollectionFormat(parameter);
                /** TODO: } else {
                 Map<PropertyId, Object> args = new HashMap<PropertyId, Object>();
                 String format = qp.getFormat();
                 args.put(PropertyId.ENUM, qp.getEnum());
                 parameterSchema = PropertyBuilder.build(type, format, args);
                 */
            }

            if (parameterSchema == null) {
                LOGGER.warn("warning!  Schema not found for parameter \"" + parameter.getName() + "\", using String");
                parameterSchema = new StringSchema().description("//TODO automatically added by swagger-codegen.");
            }
            CodegenProperty codegenProperty = fromProperty(parameter.getName(), parameterSchema);

            // set boolean flag (e.g. isString)
            setParameterBooleanFlagWithCodegenProperty(codegenParameter, codegenProperty);

            codegenParameter.dataType = codegenProperty.datatype;
            codegenParameter.dataFormat = codegenProperty.dataFormat;
            boolean isEnum = getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME);
            if(isEnum) {
                codegenParameter.datatypeWithEnum = codegenProperty.datatypeWithEnum;
                codegenParameter.enumName = codegenProperty.enumName;
            }

            // enum
            updateCodegenPropertyEnum(codegenProperty);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_ENUM_EXT_NAME, Boolean.TRUE);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenParameter._enum = codegenProperty._enum;
            codegenParameter.allowableValues = codegenProperty.allowableValues;


            if (codegenProperty.items != null && getBooleanValue(codegenProperty.items, IS_ENUM_EXT_NAME)) {
                codegenParameter.datatypeWithEnum = codegenProperty.datatypeWithEnum;
                codegenParameter.enumName = codegenProperty.enumName;
                codegenParameter.items = codegenProperty.items;
            }
            codegenParameter.collectionFormat = collectionFormat;
            if(collectionFormat != null && collectionFormat.equals("multi")) {
                codegenParameter.getVendorExtensions().put(CodegenConstants.IS_COLLECTION_FORMAT_MULTI_EXT_NAME, Boolean.TRUE);
            }
            codegenParameter.paramName = toParamName(parameter.getName());

            // import
            if (codegenProperty.complexType != null) {
                imports.add(codegenProperty.complexType);
            }

            // validation
            // handle maximum, minimum properly for int/long by removing the trailing ".0"
            if (parameterSchema instanceof IntegerSchema) {
                codegenParameter.maximum = parameterSchema.getMaximum() == null ? null : String.valueOf(parameterSchema.getMaximum().longValue());
                codegenParameter.minimum = parameterSchema.getMinimum() == null ? null : String.valueOf(parameterSchema.getMinimum().longValue());
            } else {
                codegenParameter.maximum = parameterSchema.getMaximum() == null ? null : String.valueOf(parameterSchema.getMaximum());
                codegenParameter.minimum = parameterSchema.getMinimum() == null ? null : String.valueOf(parameterSchema.getMinimum());
            }

            codegenParameter.exclusiveMaximum = parameterSchema.getExclusiveMaximum() == null ? false : parameterSchema.getExclusiveMaximum();
            codegenParameter.exclusiveMinimum = parameterSchema.getExclusiveMinimum() == null ? false : parameterSchema.getExclusiveMinimum();
            codegenParameter.maxLength = parameterSchema.getMaxLength();
            codegenParameter.minLength = parameterSchema.getMinLength();
            codegenParameter.pattern = toRegularExpression(parameterSchema.getPattern());
            codegenParameter.maxItems = parameterSchema.getMaxItems();
            codegenParameter.minItems = parameterSchema.getMinItems();
            codegenParameter.uniqueItems = parameterSchema.getUniqueItems() == null ? false : parameterSchema.getUniqueItems();
            codegenParameter.multipleOf = parameterSchema.getMultipleOf();

            // exclusive* are noop without corresponding min/max
            if (codegenParameter.maximum != null || codegenParameter.minimum != null ||
                    codegenParameter.maxLength != null || codegenParameter.minLength != null ||
                    codegenParameter.maxItems != null || codegenParameter.minItems != null ||
                    codegenParameter.pattern != null) {
                codegenParameter.getVendorExtensions().put(CodegenConstants.HAS_VALIDATION_EXT_NAME, Boolean.TRUE);
            }

        }

        // Issue #2561 (neilotoole) : Set the is<TYPE>Param flags.
        // This code has been moved to here from #fromOperation
        // because these values should be set before calling #postProcessParameter.
        // See: https://github.com/swagger-api/swagger-codegen/issues/2561
        if (parameter instanceof QueryParameter) {
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_QUERY_PARAM_EXT_NAME, Boolean.TRUE);
        } else if (parameter instanceof PathParameter) {
            codegenParameter.required = true;
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_PATH_PARAM_EXT_NAME, Boolean.TRUE);
        } else if (parameter instanceof HeaderParameter) {
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_HEADER_PARAM_EXT_NAME, Boolean.TRUE);
        } else if (parameter instanceof CookieParameter) {
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_COOKIE_PARAM_EXT_NAME, Boolean.TRUE);
        }
        /** TODO:
        else if (parameter instanceof BodyParameter) {
            codegenParameter.isBodyParam = true;
            codegenParameter.isBinary = isDataTypeBinary(codegenParameter.dataType);
        }

        else if (parameter instanceof FormParameter) {
            if ("file".equalsIgnoreCase(((FormParameter) parameter).getType()) || "file".equals(codegenParameter.baseType)) {
                codegenParameter.isFile = true;
            } else {
                codegenParameter.notFile = true;
            }
            codegenParameter.isFormParam = true;
        }
        */
        // set the example value
        // if not specified in x-example, generate a default value
        if (codegenParameter.vendorExtensions != null && codegenParameter.vendorExtensions.containsKey("x-example")) {
            codegenParameter.example = Json.pretty(codegenParameter.vendorExtensions.get("x-example"));
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_STRING_EXT_NAME)) {
            codegenParameter.example = codegenParameter.paramName + "_example";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_BOOLEAN_EXT_NAME)) {
            codegenParameter.example = "true";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_LONG_EXT_NAME)) {
            codegenParameter.example = "789";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_INTEGER_EXT_NAME)) {
            codegenParameter.example = "56";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_FLOAT_EXT_NAME)) {
            codegenParameter.example = "3.4";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_DOUBLE_EXT_NAME)) {
            codegenParameter.example = "1.2";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_BINARY_EXT_NAME)) {
            codegenParameter.example = "BINARY_DATA_HERE";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_BYTE_ARRAY_EXT_NAME)) {
            codegenParameter.example = "B";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_FILE_EXT_NAME)) {
            codegenParameter.example = "/path/to/file.txt";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_DATE_EXT_NAME)) {
            codegenParameter.example = "2013-10-20";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_DATE_TIME_EXT_NAME)) {
            codegenParameter.example = "2013-10-20T19:20:30+01:00";
        } else if (getBooleanValue(codegenParameter, CodegenConstants.IS_UUID_EXT_NAME)) {
            codegenParameter.example = "38400000-8cf0-11bd-b23e-10b96e4ef00d";
        }

        // set the parameter excample value
        // should be overridden by lang codegen
        setParameterExampleValue(codegenParameter);

        postProcessParameter(codegenParameter);
        return codegenParameter;
    }

    public CodegenParameter fromRequestBody(RequestBody body, Map<String, Schema> schemas, Set<String> imports) {
        CodegenParameter codegenParameter = CodegenModelFactory.newInstance(CodegenModelType.PARAMETER);
        codegenParameter.baseName = REQUEST_BODY_NAME;
        codegenParameter.paramName = REQUEST_BODY_NAME;
        codegenParameter.required = body.getRequired() != null ? body.getRequired() : Boolean.FALSE;
        codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BODY_PARAM_EXT_NAME, Boolean.TRUE);

        String name = null;
        Schema schema = getSchemaFromBody(body);
        if (StringUtils.isNotBlank(schema.get$ref())) {
            name = getSimpleRef(schema.get$ref());
            schema = schemas.get(name);
        }
        if ((SchemaTypeUtil.OBJECT_TYPE.equals(schema.getType())
                || (schema.getType() == null && schema.getProperties() != null && !schema.getProperties().isEmpty()))
                && !(schema instanceof MapSchema)) {
            CodegenModel codegenModel = null;
            if (StringUtils.isNotBlank(name)) {
                schema.setName(name);
                codegenModel = fromModel(name, schema, schemas);
            }
            if (codegenModel != null && !codegenModel.emptyVars) {
                codegenParameter.paramName = codegenModel.classname.toLowerCase();
                codegenParameter.dataType = getTypeDeclaration(codegenModel.classname);
                imports.add(codegenParameter.dataType);
            } else {
                CodegenProperty codegenProperty = fromProperty("property", schema);
                if (codegenProperty != null) {
                    codegenParameter.baseType = codegenProperty.baseType;
                    codegenParameter.dataType = codegenProperty.datatype;

                    boolean isPrimitiveType = getBooleanValue(codegenProperty, CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME);
                    boolean isBinary = getBooleanValue(codegenProperty, CodegenConstants.IS_BINARY_EXT_NAME);
                    boolean isFile = getBooleanValue(codegenProperty, CodegenConstants.IS_FILE_EXT_NAME);

                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, isPrimitiveType);
                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_BINARY_EXT_NAME, isBinary);
                    codegenParameter.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, isFile);

                    if (codegenProperty.complexType != null) {
                        imports.add(codegenProperty.complexType);
                    }
                }
                setParameterBooleanFlagWithCodegenProperty(codegenParameter, codegenProperty);
            }
        }
        else if (schema instanceof ArraySchema) {
            final ArraySchema arraySchema = (ArraySchema) schema;
            Schema inner = arraySchema.getItems();
            if (inner == null) {
                inner = new StringSchema().description("//TODO automatically added by swagger-codegen");
                arraySchema.setItems(inner);
            }
            CodegenProperty codegenProperty = fromProperty("inner", inner);
            if (codegenProperty.complexType != null) {
                imports.add(codegenProperty.complexType);
            }
            imports.add(codegenProperty.baseType);
            CodegenProperty innerCp = codegenProperty;
            while(innerCp != null) {
                if(innerCp.complexType != null) {
                    imports.add(innerCp.complexType);
                }
                innerCp = innerCp.items;
            }
            codegenParameter.items = codegenProperty;
            codegenParameter.dataType = codegenProperty.datatype;
            codegenParameter.baseType = codegenProperty.complexType;
            boolean isPrimitiveType = getBooleanValue(codegenProperty, CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, isPrimitiveType);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_CONTAINER_EXT_NAME, Boolean.TRUE);
            codegenParameter.getVendorExtensions().put(CodegenConstants.IS_LIST_CONTAINER_EXT_NAME, Boolean.TRUE);

            setParameterBooleanFlagWithCodegenProperty(codegenParameter, codegenProperty);

            while (codegenProperty != null) {
                imports.add(codegenProperty.baseType);
                codegenProperty = codegenProperty.items;
            }
        }
        return codegenParameter;
    }

    public boolean isDataTypeBinary(String dataType) {
        if (dataType != null) {
            return dataType.toLowerCase().startsWith("byte");
        } else {
            return false;
        }
    }

    public boolean isDataTypeFile(String dataType) {
        if (dataType != null) {
            return dataType.toLowerCase().equals("file");
        } else {
            return false;
        }
    }

    /**
     * Convert map of Swagger SecurityScheme objects to a list of Codegen Security objects
     *
     * @param securitySchemeMap a map of Swagger SecuritySchemeDefinition object
     * @return a list of Codegen Security objects
     */
    @SuppressWarnings("static-method")
    public List<CodegenSecurity> fromSecurity(Map<String, SecurityScheme> securitySchemeMap) {
        if (securitySchemeMap == null) {
            return Collections.emptyList();
        }

        List<CodegenSecurity> securities = new ArrayList<CodegenSecurity>(securitySchemeMap.size());
        for (String key : securitySchemeMap.keySet()) {
            final SecurityScheme schemeDefinition = securitySchemeMap.get(key);

            CodegenSecurity codegenSecurity = CodegenModelFactory.newInstance(CodegenModelType.SECURITY);
            codegenSecurity.name = key;
            codegenSecurity.type = schemeDefinition.getType().toString();

            if (SecurityScheme.Type.APIKEY.equals(schemeDefinition.getType())) {
                codegenSecurity.keyParamName = schemeDefinition.getName();
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_API_KEY_EXT_NAME, Boolean.TRUE);

                boolean isKeyInHeader = schemeDefinition.getIn() == SecurityScheme.In.HEADER;
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_KEY_IN_HEADER_EXT_NAME, isKeyInHeader);
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_KEY_IN_QUERY_EXT_NAME, !isKeyInHeader);

            } else if (SecurityScheme.Type.HTTP.equals(schemeDefinition.getType())) {
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_BASIC_EXT_NAME, Boolean.TRUE);
            } else if (SecurityScheme.Type.OAUTH2.equals(schemeDefinition.getType())) {
                codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_OAUTH_EXT_NAME, Boolean.TRUE);
                final OAuthFlows flows = schemeDefinition.getFlows();
                if (schemeDefinition.getFlows() == null) {
                    throw new RuntimeException("missing oauth flow in " + codegenSecurity.name);
                }
                if(flows.getPassword() != null) {
                    setOauth2Info(codegenSecurity, flows.getPassword());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_PASSWORD_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "password";
                }
                else if(flows.getImplicit() != null) {
                    setOauth2Info(codegenSecurity, flows.getImplicit());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_IMPLICIT_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "implicit";
                }
                else if(flows.getClientCredentials() != null) {
                    setOauth2Info(codegenSecurity, flows.getClientCredentials());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_APPLICATION_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "application";
                }
                else if(flows.getAuthorizationCode() != null) {
                    setOauth2Info(codegenSecurity, flows.getAuthorizationCode());
                    codegenSecurity.getVendorExtensions().put(CodegenConstants.IS_CODE_EXT_NAME, Boolean.TRUE);
                    codegenSecurity.flow = "accessCode";
                }
                else {
                    throw new RuntimeException("Could not identify any oauth2 flow in " + codegenSecurity.name);
                }
            }

            securities.add(codegenSecurity);
        }

        // sort auth methods to maintain the same order
        Collections.sort(securities, new Comparator<CodegenSecurity>() {
            @Override
            public int compare(CodegenSecurity one, CodegenSecurity another) {
                return ObjectUtils.compare(one.name, another.name);
            }
        });
        // set 'hasMore'
        Iterator<CodegenSecurity> it = securities.iterator();
        while (it.hasNext()) {
            final CodegenSecurity security = it.next();
            security.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, it.hasNext());
        }

        return securities;
    }

    protected void setReservedWordsLowerCase(List<String> words) {
        reservedWords = new HashSet<String>();
        for (String word : words) {
            reservedWords.add(word.toLowerCase());
        }
    }

    protected boolean isReservedWord(String word) {
        return word != null && reservedWords.contains(word.toLowerCase());
    }

    /**
     * Get operationId from the operation object, and if it's blank, generate a new one from the given parameters.
     *
     * @param operation the operation object
     * @param path the path of the operation
     * @param httpMethod the HTTP method of the operation
     * @return the (generated) operationId
     */
    protected String getOrGenerateOperationId(Operation operation, String path, String httpMethod) {
        String operationId = operation.getOperationId();
        if (StringUtils.isBlank(operationId)) {
            String tmpPath = path;
            tmpPath = tmpPath.replaceAll("\\{", "");
            tmpPath = tmpPath.replaceAll("\\}", "");
            String[] parts = (tmpPath + "/" + httpMethod).split("/");
            StringBuilder builder = new StringBuilder();
            if ("/".equals(tmpPath)) {
                // must be root tmpPath
                builder.append("root");
            }
            for (String part : parts) {
                if (part.length() > 0) {
                    if (builder.toString().length() == 0) {
                        part = Character.toLowerCase(part.charAt(0)) + part.substring(1);
                    } else {
                        part = initialCaps(part);
                    }
                    builder.append(part);
                }
            }
            operationId = sanitizeName(builder.toString());
            LOGGER.warn("Empty operationId found for path: " + httpMethod + " " + path + ". Renamed to auto-generated operationId: " + operationId);
        }
        return operationId;
    }

    /**
     * Check the type to see if it needs import the library/module/package
     *
     * @param type name of the type
     * @return true if the library/module/package of the corresponding type needs to be imported
     */
    protected boolean needToImport(String type) {
        return StringUtils.isNotBlank(type) && !defaultIncludes.contains(type)
                && !languageSpecificPrimitives.contains(type);
    }

    @SuppressWarnings("static-method")
    protected List<Map<String, Object>> toExamples(Map<String, Object> examples) {
        if (examples == null) {
            return null;
        }

        final List<Map<String, Object>> output = new ArrayList<Map<String, Object>>(examples.size());
        for (Map.Entry<String, Object> entry : examples.entrySet()) {
            final Map<String, Object> kv = new HashMap<String, Object>();
            kv.put("contentType", entry.getKey());
            kv.put("example", entry.getValue());
            output.add(kv);
        }
        return output;
    }

    private void addHeaders(ApiResponse response, List<CodegenProperty> target) {
        if (response.getHeaders() != null) {
            for (Map.Entry<String, Header> headers : response.getHeaders().entrySet()) {
                target.add(fromProperty(headers.getKey(), headers.getValue().getSchema()));
            }
        }
    }

    private static List<CodegenParameter> addHasMore(List<CodegenParameter> objs) {
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                if (i > 0) {
                    objs.get(i).secondaryParam = true;
                }
                if (i < objs.size() - 1) {
                    objs.get(i).getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.TRUE);
                }
            }
        }
        return objs;
    }

    private static Map<String, Object> addHasMore(Map<String, Object> objs) {
        if (objs != null) {
            for (int i = 0; i < objs.size() - 1; i++) {
                if (i > 0) {
                    objs.put("secondaryParam", true);
                }
                if (i < objs.size() - 1) {
                    objs.put("hasMore", true);
                }
            }
        }
        return objs;
    }

    /**
     * Add operation to group
     *
     * @param tag name of the tag
     * @param resourcePath path of the resource
     * @param operation Swagger Operation object
     * @param co Codegen Operation object
     * @param operations map of Codegen operations
     */
    @SuppressWarnings("static-method")
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        List<CodegenOperation> opList = operations.get(tag);
        if (opList == null) {
            opList = new ArrayList<CodegenOperation>();
            operations.put(tag, opList);
        }
        // check for operationId uniqueness

        String uniqueName = co.operationId;
        int counter = 0;
        for(CodegenOperation op : opList) {
            if(uniqueName.equals(op.operationId)) {
                uniqueName = co.operationId + "_" + counter;
                counter ++;
            }
        }
        if(!co.operationId.equals(uniqueName)) {
            LOGGER.warn("generated unique operationId `" + uniqueName + "`");
        }
        co.operationId = uniqueName;
        co.operationIdLowerCase = uniqueName.toLowerCase();
        co.operationIdCamelCase = DefaultCodegen.camelize(uniqueName);
        co.operationIdSnakeCase = DefaultCodegen.underscore(uniqueName);
        opList.add(co);
        co.baseName = tag;
    }

    private void addParentContainer(CodegenModel codegenModel, String name, Schema property) {
        final CodegenProperty codegenProperty = fromProperty(name, property);
        addImport(codegenModel, codegenProperty.complexType);
        codegenModel.parent = toInstantiationType(property);
        final String containerType = codegenProperty.containerType;
        final String instantiationType = instantiationTypes.get(containerType);
        if (instantiationType != null) {
            addImport(codegenModel, instantiationType);
        }
        final String mappedType = typeMapping.get(containerType);
        if (mappedType != null) {
            addImport(codegenModel, mappedType);
        }
    }

    /**
     * Underscore the given word.
     * Copied from Twitter elephant bird
     * https://github.com/twitter/elephant-bird/blob/master/core/src/main/java/com/twitter/elephantbird/util/Strings.java
     *
     * @param word The word
     * @return The underscored version of the word
     */
    public static String underscore(String word) {
        String firstPattern = "([A-Z]+)([A-Z][a-z])";
        String secondPattern = "([a-z\\d])([A-Z])";
        String replacementPattern = "$1_$2";
        // Replace package separator with slash.
        word = word.replaceAll("\\.", "/"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        // Replace $ with two underscores for inner classes.
        word = word.replaceAll("\\$", "__");
        // Replace capital letter with _ plus lowercase letter.
        word = word.replaceAll(firstPattern, replacementPattern);
        word = word.replaceAll(secondPattern, replacementPattern);
        word = word.replace('-', '_');
        // replace space with underscore
        word = word.replace(' ', '_');
        word = word.toLowerCase();
        return word;
    }

    /**
     * Dashize the given word.
     *
     * @param word The word
     * @return The dashized version of the word, e.g. "my-name"
     */
    @SuppressWarnings("static-method")
    protected String dashize(String word) {
        return underscore(word).replaceAll("[_ ]", "-");
    }

    /**
     * Generate the next name for the given name, i.e. append "2" to the base name if not ending with a number,
     * otherwise increase the number by 1. For example:
     *   status    => status2
     *   status2   => status3
     *   myName100 => myName101
     *
     * @param name The base name
     * @return The next name for the base name
     */
    private static String generateNextName(String name) {
        Pattern pattern = Pattern.compile("\\d+\\z");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            String numStr = matcher.group();
            int num = Integer.parseInt(numStr) + 1;
            return name.substring(0, name.length() - numStr.length()) + num;
        } else {
            return name + "2";
        }
    }

    protected void addImport(CodegenModel m, String type) {
        if (type != null && needToImport(type)) {
            m.imports.add(type);
        }
    }

    private void addVars(CodegenModel codegenModel, Map<String, Schema> properties, List<String> required) {
        addVars(codegenModel, properties, required, null, null);
    }

    private void addVars(CodegenModel codegenModel, Map<String, Schema> properties, List<String> required, Map<String, Schema> allProperties, List<String> allRequired) {

        codegenModel.getVendorExtensions().put(CodegenConstants.HAS_REQUIRED_EXT_NAME, Boolean.FALSE);
        if (properties != null && !properties.isEmpty()) {
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, true);
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);

            Set<String> mandatory = required == null ? Collections.<String> emptySet()
                    : new TreeSet<String>(required);
            addVars(codegenModel, codegenModel.vars, properties, mandatory);
            codegenModel.allMandatory = codegenModel.mandatory = mandatory;
        } else {
            codegenModel.emptyVars = true;
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_VARS_EXT_NAME, false);
            codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, false);
        }

        if (allProperties != null) {
            Set<String> allMandatory = allRequired == null ? Collections.<String> emptySet()
                    : new TreeSet<String>(allRequired);
            addVars(codegenModel, codegenModel.allVars, allProperties, allMandatory);
            codegenModel.allMandatory = allMandatory;
        }
    }

    private void addVars(CodegenModel codegenModel, List<CodegenProperty> vars, Map<String, Schema> properties, Set<String> mandatory) {
        // convert set to list so that we can access the next entry in the loop
        List<Map.Entry<String, Schema>> propertyList = new ArrayList<Map.Entry<String, Schema>>(properties.entrySet());
        final int totalCount = propertyList.size();
        for (int i = 0; i < totalCount; i++) {
            Map.Entry<String, Schema> entry = propertyList.get(i);

            final String key = entry.getKey();
            final Schema propertySchema = entry.getValue();

            if (propertySchema == null) {
                LOGGER.warn("null property for " + key);
            } else {
                final CodegenProperty cp = fromProperty(key, propertySchema);
                cp.required = mandatory.contains(key);

                boolean hasRequired = getBooleanValue(codegenModel, HAS_REQUIRED_EXT_NAME) || cp.required;
                boolean hasOptional = getBooleanValue(codegenModel, HAS_OPTIONAL_EXT_NAME) || !cp.required;

                codegenModel.getVendorExtensions().put(HAS_REQUIRED_EXT_NAME, hasRequired);
                codegenModel.getVendorExtensions().put(HAS_OPTIONAL_EXT_NAME, hasOptional);

                boolean isEnum = getBooleanValue(cp, IS_ENUM_EXT_NAME);
                if (isEnum) {
                    // FIXME: if supporting inheritance, when called a second time for allProperties it is possible for
                    // m.hasEnums to be set incorrectly if allProperties has enumerations but properties does not.
                    codegenModel.getVendorExtensions().put(CodegenConstants.HAS_ENUMS_EXT_NAME, true);
                }

                // set model's hasOnlyReadOnly to false if the property is read-only
                if (!getBooleanValue(cp, CodegenConstants.IS_READ_ONLY_EXT_NAME)) {
                    codegenModel.getVendorExtensions().put(HAS_ONLY_READ_ONLY_EXT_NAME, Boolean.FALSE);
                }

                if (i+1 != totalCount) {
                    cp.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.TRUE);
                    // check the next entry to see if it's read only
                    if (!Boolean.TRUE.equals(propertyList.get(i+1).getValue().getReadOnly())) {
                        cp.getVendorExtensions().put(CodegenConstants.HAS_MORE_NON_READ_ONLY_EXT_NAME, Boolean.TRUE);
                    }
                }

                if (getBooleanValue(cp, CodegenConstants.IS_CONTAINER_EXT_NAME)) {
                    addImport(codegenModel, typeMapping.get("array"));
                }

                addImport(codegenModel, cp.baseType);
                CodegenProperty innerCp = cp;
                while(innerCp != null) {
                    addImport(codegenModel, innerCp.complexType);
                    innerCp = innerCp.items;
                }
                vars.add(cp);

                // if required, add to the list "requiredVars"
                if (Boolean.TRUE.equals(cp.required)) {
                    codegenModel.requiredVars.add(cp);
                } else { // else add to the list "optionalVars" for optional property
                    codegenModel.optionalVars.add(cp);
                }

                // if readonly, add to readOnlyVars (list of properties)
                if (getBooleanValue(cp, CodegenConstants.IS_READ_ONLY_EXT_NAME)) {
                    codegenModel.readOnlyVars.add(cp);
                } else { // else add to readWriteVars (list of properties)
                    // FIXME: readWriteVars can contain duplicated properties. Debug/breakpoint here while running C# generator (Dog and Cat models)
                    codegenModel.readWriteVars.add(cp);
                }
            }
        }
    }

    /**
     * Determine all of the types in the model definitions that are aliases of
     * simple types.
     * @param allSchemas The complete set of model definitions.
     * @return A mapping from model name to type alias
     */
    private static Map<String, String> getAllAliases(Map<String, Schema> allSchemas) {
        Map<String, String> aliases = new HashMap<>();
        if (allSchemas == null || allSchemas.isEmpty()) {
            return aliases;
        }
        for (Map.Entry<String, Schema> entry : allSchemas.entrySet()) {
            String swaggerName = entry.getKey();
            Schema schema = entry.getValue();
            if (schema.getType() != null && !schema.getType().equals("object") && schema.getEnum() == null) {
                aliases.put(swaggerName, schema.getType());
            }
        }
        return aliases;
    }

    /**
     * Remove characters not suitable for variable or method name from the input and camelize it
     *
     * @param name string to be camelize
     * @return camelized string
     */
    @SuppressWarnings("static-method")
    public String removeNonNameElementToCamelCase(String name) {
        return removeNonNameElementToCamelCase(name, "[-_:;#]");
    }

    /**
     * Remove characters that is not good to be included in method name from the input and camelize it
     *
     * @param name string to be camelize
     * @param nonNameElementPattern a regex pattern of the characters that is not good to be included in name
     * @return camelized string
     */
    protected String removeNonNameElementToCamelCase(final String name, final String nonNameElementPattern) {
        String result = Arrays.stream(name.split(nonNameElementPattern))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(""));
        if (result.length() > 0) {
            result = result.substring(0, 1).toLowerCase() + result.substring(1);
        }
        return result;
    }

    /**
     * Camelize name (parameter, property, method, etc) with upper case for first letter
     * copied from Twitter elephant bird
     * https://github.com/twitter/elephant-bird/blob/master/core/src/main/java/com/twitter/elephantbird/util/Strings.java
     *
     * @param word string to be camelize
     * @return camelized string
     */
    public static String camelize(String word) {
        return camelize(word, false);
    }

    /**
     * Camelize name (parameter, property, method, etc)
     *
     * @param word string to be camelize
     * @param lowercaseFirstLetter lower case for first letter if set to true
     * @return camelized string
     */
    public static String camelize(String word, boolean lowercaseFirstLetter) {
        // Replace all slashes with dots (package separator)
        Pattern p = Pattern.compile("\\/(.?)");
        Matcher m = p.matcher(word);
        while (m.find()) {
            word = m.replaceFirst("." + m.group(1)/*.toUpperCase()*/); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
            m = p.matcher(word);
        }

        // case out dots
        String[] parts = word.split("\\.");
        StringBuilder f = new StringBuilder();
        for (String z : parts) {
            if (z.length() > 0) {
                f.append(Character.toUpperCase(z.charAt(0))).append(z.substring(1));
            }
        }
        word = f.toString();

        m = p.matcher(word);
        while (m.find()) {
            word = m.replaceFirst("" + Character.toUpperCase(m.group(1).charAt(0)) + m.group(1).substring(1)/*.toUpperCase()*/);
            m = p.matcher(word);
        }

        // Uppercase the class name.
        p = Pattern.compile("(\\.?)(\\w)([^\\.]*)$");
        m = p.matcher(word);
        if (m.find()) {
            String rep = m.group(1) + m.group(2).toUpperCase() + m.group(3);
            rep = rep.replaceAll("\\$", "\\\\\\$");
            word = m.replaceAll(rep);
        }

        // Remove all underscores (underscore_case to camelCase)
        p = Pattern.compile("(_)(.)");
        m = p.matcher(word);
        while (m.find()) {
            String original = m.group(2);
            String upperCase = original.toUpperCase();
            if (original.equals(upperCase)) {
                word = word.replaceFirst("_", "");
            } else {
                word = m.replaceFirst(upperCase);
            }
            m = p.matcher(word);
        }

        // Remove all hyphens (hyphen-case to camelCase)
        p = Pattern.compile("(-)(.)");
        m = p.matcher(word);
        while (m.find()) {
            word = m.replaceFirst(m.group(2).toUpperCase());
            m = p.matcher(word);
        }

        if (lowercaseFirstLetter && word.length() > 0) {
            word = word.substring(0, 1).toLowerCase() + word.substring(1);
        }

        return word;
    }

    public String apiFilename(String templateName, String tag) {
        String suffix = apiTemplateFiles().get(templateName);
        return apiFileFolder() + '/' + toApiFilename(tag) + suffix;
    }

    /**
     * Return the full path and API documentation file
     *
     * @param templateName template name
     * @param tag tag
     *
     * @return the API documentation file name with full path
     */
    public String apiDocFilename(String templateName, String tag) {
        String suffix = apiDocTemplateFiles().get(templateName);
        return apiDocFileFolder() + '/' + toApiDocFilename(tag) + suffix;
    }

    /**
     * Return the full path and API test file
     *
     * @param templateName template name
     * @param tag tag
     *
     * @return the API test file name with full path
     */
    public String apiTestFilename(String templateName, String tag) {
        String suffix = apiTestTemplateFiles().get(templateName);
        return apiTestFileFolder() + '/' + toApiTestFilename(tag) + suffix;
    }

    public boolean shouldOverwrite(String filename) {
        return !(skipOverwrite && new File(filename).exists());
    }

    public boolean isSkipOverwrite() {
        return skipOverwrite;
    }

    public void setSkipOverwrite(boolean skipOverwrite) {
        this.skipOverwrite = skipOverwrite;
    }

    public boolean isRemoveOperationIdPrefix() {
        return removeOperationIdPrefix;
    }

    public void setRemoveOperationIdPrefix(boolean removeOperationIdPrefix) {
        this.removeOperationIdPrefix = removeOperationIdPrefix;
    }

    /**
     * All library templates supported.
     * (key: library name, value: library description)
     * @return the supported libraries
     */
    public Map<String, String> supportedLibraries() {
        return supportedLibraries;
    }

    /**
     * Set library template (sub-template).
     *
     * @param library Library template
     */
    public void setLibrary(String library) {
        if (library != null && !supportedLibraries.containsKey(library)) {
            StringBuilder sb = new StringBuilder("Unknown library: " + library + "\nAvailable libraries:");
            if(supportedLibraries.size() == 0) {
                sb.append("\n  ").append("NONE");
            } else {
                for (String lib : supportedLibraries.keySet()) {
                    sb.append("\n  ").append(lib);
                }
            }
            throw new RuntimeException(sb.toString());
        }
        this.library = library;
    }

    /**
     * Library template (sub-template).
     *
     * @return Library template
     */
    public String getLibrary() {
        return library;
    }

    /**
     * Set Git user ID.
     *
     * @param gitUserId Git user ID
     */
    public void setGitUserId(String gitUserId) {
        this.gitUserId = gitUserId;
    }

    /**
     * Git user ID
     *
     * @return Git user ID
     */
    public String getGitUserId() {
        return gitUserId;
    }

    /**
     * Set Git repo ID.
     *
     * @param gitRepoId Git repo ID
     */
    public void setGitRepoId(String gitRepoId) {
        this.gitRepoId = gitRepoId;
    }

    /**
     * Git repo ID
     *
     * @return Git repo ID
     */
    public String getGitRepoId() {
        return gitRepoId;
    }

    /**
     * Set release note.
     *
     * @param releaseNote Release note
     */
    public void setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
    }

    /**
     * Release note
     *
     * @return Release note
     */
    public String getReleaseNote() {
        return releaseNote;
    }

    /**
     * Set HTTP user agent.
     *
     * @param httpUserAgent HTTP user agent
     */
    public void setHttpUserAgent(String httpUserAgent) {
        this.httpUserAgent = httpUserAgent;
    }

    /**
     * HTTP user agent
     *
     * @return HTTP user agent
     */
    public String getHttpUserAgent() {
        return httpUserAgent;
    }

    @SuppressWarnings("static-method")
    protected CliOption buildLibraryCliOption(Map<String, String> supportedLibraries) {
        StringBuilder sb = new StringBuilder("library template (sub-template) to use:");
        for (String lib : supportedLibraries.keySet()) {
            sb.append("\n").append(lib).append(" - ").append(supportedLibraries.get(lib));
        }
        return new CliOption("library", sb.toString());
    }

    /**
     * Sanitize name (parameter, property, method, etc)
     *
     * @param name string to be sanitize
     * @return sanitized string
     */
    @SuppressWarnings("static-method")
    public String sanitizeName(String name) {
        // NOTE: performance wise, we should have written with 2 replaceAll to replace desired
        // character with _ or empty character. Below aims to spell out different cases we've
        // encountered so far and hopefully make it easier for others to add more special
        // cases in the future.

        // better error handling when map/array type is invalid
        if (name == null) {
            LOGGER.error("String to be sanitized is null. Default to " + Object.class.getSimpleName());
            return Object.class.getSimpleName();
        }

        // if the name is just '$', map it to 'value' for the time being.
        if ("$".equals(name)) {
            return "value";
        }

        // input[] => input
        name = name.replaceAll("\\[\\]", ""); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // input[a][b] => input_a_b
        name = name.replaceAll("\\[", "_");
        name = name.replaceAll("\\]", "");

        // input(a)(b) => input_a_b
        name = name.replaceAll("\\(", "_");
        name = name.replaceAll("\\)", "");

        // input.name => input_name
        name = name.replaceAll("\\.", "_");

        // input-name => input_name
        name = name.replaceAll("-", "_");

        // input name and age => input_name_and_age
        name = name.replaceAll(" ", "_");

        // remove everything else other than word, number and _
        // $php_variable => php_variable
        if (allowUnicodeIdentifiers) { //could be converted to a single line with ?: operator
            name = Pattern.compile("\\W", Pattern.UNICODE_CHARACTER_CLASS).matcher(name).replaceAll("");
        }
        else {
            name = name.replaceAll("\\W", "");
        }

        return name;
    }

    /**
     * Sanitize tag
     *
     * @param tag Tag
     * @return Sanitized tag
     */
    public String sanitizeTag(String tag) {
        tag = camelize(sanitizeName(tag));

        // tag starts with numbers
        if (tag.matches("^\\d.*")) {
            tag = "Class" + tag;
        }

        return tag;
    }

    @Override
    public void addHandlebarHelpers(Handlebars handlebars) {
        handlebars.registerHelper(IsHelper.NAME, new IsHelper());
        handlebars.registerHelper(HasHelper.NAME, new HasHelper());
        handlebars.registerHelper(IsNotHelper.NAME, new IsNotHelper());
        handlebars.registerHelper(HasNotHelper.NAME, new HasNotHelper());
    }

    @Override
    public List<CodegenArgument> readLanguageArguments() {
        return null;
    }

    @Override
    public List<CodegenArgument> getLanguageArguments() {
        return null;
    }

    @Override
    public void setLanguageArguments(List<CodegenArgument> codegenArguments){
    }

    /**
     * Only write if the file doesn't exist
     *
     * @param outputFolder Output folder
     * @param supportingFile Supporting file
     */
    public void writeOptional(String outputFolder, SupportingFile supportingFile) {
        String folder = "";

        if(outputFolder != null && !"".equals(outputFolder)) {
            folder += outputFolder + File.separator;
        }
        folder += supportingFile.folder;
        if(!"".equals(folder)) {
            folder += File.separator + supportingFile.destinationFilename;
        }
        else {
            folder = supportingFile.destinationFilename;
        }
        if(!new File(folder).exists()) {
            supportingFiles.add(supportingFile);
        } else {
            LOGGER.info("Skipped overwriting " + supportingFile.destinationFilename + " as the file already exists in " + folder);
        }
    }

    /**
     * Set CodegenParameter boolean flag using CodegenProperty.
     *
     * @param parameter Codegen Parameter
     * @param property  Codegen property
     */
    public void setParameterBooleanFlagWithCodegenProperty(CodegenParameter parameter, CodegenProperty property) {
        if (parameter == null) {
            LOGGER.error("Codegen Parameter cannot be null.");
            return;
        }

        if (property == null) {
            LOGGER.error("Codegen Property cannot be null.");
            return;
        }

        parameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.TRUE);

        if (getBooleanValue(property, CodegenConstants.IS_UUID_EXT_NAME)
                && getBooleanValue(property, CodegenConstants.IS_STRING_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_UUID_EXT_NAME, Boolean.TRUE);
            parameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.FALSE);
        } else if (getBooleanValue(property, CodegenConstants.IS_BYTE_ARRAY_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_STRING_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_STRING_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_BOOLEAN_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_BOOLEAN_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_LONG_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_LONG_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_INTEGER_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_INTEGER_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_DOUBLE_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_DOUBLE_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_FLOAT_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_FLOAT_EXT_NAME, Boolean.TRUE);
        }  else if (getBooleanValue(property, CodegenConstants.IS_NUMBER_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_NUMBER_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_BINARY_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_BYTE_ARRAY_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_FILE_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_FILE_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_DATE_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_DATE_EXT_NAME, Boolean.TRUE);
        } else if (getBooleanValue(property, CodegenConstants.IS_DATE_TIME_EXT_NAME)) {
            parameter.getVendorExtensions().put(CodegenConstants.IS_DATE_TIME_EXT_NAME, Boolean.TRUE);
        } else {
            LOGGER.debug("Property type is not primitive: " + property.datatype);
            parameter.getVendorExtensions().put(CodegenConstants.IS_PRIMITIVE_TYPE_EXT_NAME, Boolean.FALSE);
        }
    }

    /**
     * If the pattern misses the delimiter, add "/" to the beginning and end
     * Otherwise, return the original pattern
     *
     * @param pattern the pattern (regular expression)
     * @return the pattern with delimiter
     */
    public String addRegularExpressionDelimiter(String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            return pattern;
        }

        if (!pattern.matches("^/.*")) {
            return "/" + pattern.replaceAll("/", "\\\\/") + "/";
        }

        return pattern;
    }

    /**
     * reads propertyKey from additionalProperties, converts it to a boolean and
     * writes it back to additionalProperties to be usable as a boolean in
     * mustache files.
     *
     * @param propertyKey property key
     * @return property value as boolean
     */
    public boolean convertPropertyToBooleanAndWriteBack(String propertyKey) {
        boolean booleanValue = false;
        if (additionalProperties.containsKey(propertyKey)) {
            booleanValue = convertPropertyToBoolean(propertyKey);
            // write back as boolean
            writePropertyBack(propertyKey, booleanValue);
        }

        return booleanValue;
    }

    /**
     * Provides an override location, if any is specified, for the .swagger-codegen-ignore.
     *
     * This is originally intended for the first generation only.
     *
     * @return a string of the full path to an override ignore file.
     */
    public String getIgnoreFilePathOverride() {
        return ignoreFilePathOverride;
    }

    /**
     * Sets an override location for the .swagger-codegen.ignore location for the first code generation.
     *
     * @param ignoreFileOverride The full path to an ignore file
     */
    public void setIgnoreFilePathOverride(final String ignoreFileOverride) {
        this.ignoreFilePathOverride = ignoreFileOverride;
    }

    public boolean convertPropertyToBoolean(String propertyKey) {
        boolean booleanValue = false;
        if (additionalProperties.containsKey(propertyKey)) {
            booleanValue = Boolean.valueOf(additionalProperties.get(propertyKey).toString());
        }

        return booleanValue;
    }

    public void writePropertyBack(String propertyKey, boolean value) {
        additionalProperties.put(propertyKey, value);
    }

    protected String getContentType(RequestBody requestBody) {
        if (requestBody == null || requestBody.getContent() == null || requestBody.getContent().isEmpty()) {
            return null;
        }
        return new ArrayList<>(requestBody.getContent().keySet()).get(0);
    }

    protected Schema getSchemaFromBody(RequestBody requestBody) {
        String contentType = new ArrayList<>(requestBody.getContent().keySet()).get(0);
        MediaType mediaType = requestBody.getContent().get(contentType);
        return mediaType.getSchema();
    }

    protected Schema getSchemaFromResponse(ApiResponse response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            return null;
        }
        Schema schema = null;
        for (MediaType mediaType : response.getContent().values()) {
            schema = mediaType.getSchema();
            break;
        }
        return schema;
    }

    protected Parameter getParameterFromRef(String ref, OpenAPI openAPI) {
        String parameterName = ref.substring(ref.lastIndexOf('/') + 1);
        Map<String, Parameter> parameterMap = openAPI.getComponents().getParameters();
        return parameterMap.get(parameterName);
    }

    private void setOauth2Info(CodegenSecurity codegenSecurity, OAuthFlow flow) {
        codegenSecurity.authorizationUrl = flow.getAuthorizationUrl();
        codegenSecurity.tokenUrl = flow.getTokenUrl();
        codegenSecurity.scopes = flow.getScopes();
    }

    private List<Schema> getInterfaces(ComposedSchema composed) {
        if(composed.getAllOf() != null && composed.getAllOf().size() > 1) {
            return composed.getAllOf().subList(1, composed.getAllOf().size());
        } else if(composed.getAnyOf() != null && !composed.getAnyOf().isEmpty()) {
            return composed.getAnyOf();
        } else if(composed.getOneOf() != null && !composed.getOneOf().isEmpty()) {
            return composed.getOneOf();
        } else {
            return null;
        }
    }

    protected void addConsumesInfo(Operation operation, CodegenOperation codegenOperation) {
        if(operation.getRequestBody() == null || operation.getRequestBody().getContent() == null || operation.getRequestBody().getContent().isEmpty()) {
            return;
        }
        Set<String> consumes = operation.getRequestBody().getContent().keySet();
        List<Map<String, String>> mediaTypeList = new ArrayList<>();
        int count = 0;
        for (String key : consumes) {
            Map<String, String> mediaType = new HashMap<>();
            if ("*/*".equals(key)) {
                mediaType.put("mediaType", key);
            } else {
                mediaType.put("mediaType", escapeText(escapeQuotationMark(key)));
            }
            count += 1;
            if (count < consumes.size()) {
                mediaType.put("hasMore", "true");
            } else {
                mediaType.put("hasMore", null);
            }
            mediaTypeList.add(mediaType);
        }
        codegenOperation.consumes = mediaTypeList;
        codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_CONSUMES_EXT_NAME, Boolean.TRUE);
    }

    public static Set<String> getConsumesInfo(Operation operation) {
        if(operation.getRequestBody() == null || operation.getRequestBody().getContent() == null || operation.getRequestBody().getContent().isEmpty()) {
            return null;
        }
        return operation.getRequestBody().getContent().keySet();
    }

    protected void addProducesInfo(ApiResponse response, CodegenOperation codegenOperation) {
        if(response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return;
        }
        Set<String> produces = response.getContent().keySet();
        if(codegenOperation.produces == null) {
            codegenOperation.produces = new ArrayList<>();
        }
        int count = 0;
        for (String key : produces) {
            Map<String, String> mediaType = new HashMap<String, String>();
            // escape quotation to avoid code injection
            if ("*/*".equals(key)) { // "*/*" is a special case, do nothing
                mediaType.put("mediaType", key);
            } else {
                mediaType.put("mediaType", escapeText(escapeQuotationMark(key)));
            }
            count += 1;
            if (count < produces.size()) {
                mediaType.put("hasMore", "true");
            } else {
                mediaType.put("hasMore", null);
            }
            codegenOperation.produces.add(mediaType);
            codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_PRODUCES_EXT_NAME, Boolean.TRUE);
        }
    }


    public static Set<String> getProducesInfo(Operation operation) {
        if(operation.getResponses() == null || operation.getResponses().isEmpty()) {
            return null;
        }
        return operation.getResponses().keySet();
    }

    protected Schema detectParent(ComposedSchema composedSchema, Map<String, Schema> allSchemas) {
        if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
            Schema schema = composedSchema.getAllOf().get(0);
            String ref = schema.get$ref();
            if (StringUtils.isBlank(ref)) {
                return null;
            }
            ref = getSimpleRef(ref);
            return allSchemas.get(ref);
        }
        return null;
    }

    protected String getParentName(ComposedSchema composedSchema, Map<String, Schema> allSchemas) {
        if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
            Schema schema = composedSchema.getAllOf().get(0);
            String ref = schema.get$ref();
            if (StringUtils.isBlank(ref)) {
                return null;
            }
            return getSimpleRef(ref);
        }
        return null;
    }

    protected String getSimpleRef(String ref) {
        if (ref.startsWith("#/components/schemas/")) {
            ref = ref.substring(ref.lastIndexOf("/") + 1);
        }
        return ref;
    }

    protected String getCollectionFormat(Parameter parameter) {
        if (Parameter.StyleEnum.FORM.equals(parameter.getStyle())) {
            if (parameter.getExplode() != null && parameter.getExplode()) {
                return "csv";
            } else {
                return "multi";
            }
        }
        else if (Parameter.StyleEnum.PIPEDELIMITED.equals(parameter.getStyle())) {
            return "pipe";
        }
        else if (Parameter.StyleEnum.SPACEDELIMITED.equals(parameter.getStyle())) {
            return "space";
        }
        else {
            return null;
        }
    }

    protected static boolean hasSchemaProperties(Schema schema) {
        final Object additionalProperties = schema.getAdditionalProperties();
        return additionalProperties != null && additionalProperties instanceof Schema;
    }

    public CodegenType getTag() {
        return null;
    }

    public String getName() {
        return null;
    }

    public String getHelp() {
        return null;
    }
}