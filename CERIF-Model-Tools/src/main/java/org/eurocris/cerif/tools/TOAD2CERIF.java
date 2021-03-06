package org.eurocris.cerif.tools;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.eurocris.cerif.CERIFClassScheme;
import org.eurocris.cerif.model.Attribute;
import org.eurocris.cerif.model.CERIFEntityType;
import org.eurocris.cerif.model.Entity;
import org.eurocris.cerif.model.Model;
import org.eurocris.cerif.model.Relationship;
import org.eurocris.cerif.model.toad.ToadModelParser;
import org.eurocris.cerif.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TOAD2CERIF {

	public static final String CERIF_ENTITIES_UUID = Entity.class.getAnnotation( CERIFClassScheme.class ).id();
	public static final String CERIF_ENTITY_TYPES_UUID = CERIFEntityType.class.getAnnotation( CERIFClassScheme.class ).id();
	public static final String CERIF_ATTRIBUTES_UUID = Attribute.class.getAnnotation( CERIFClassScheme.class ).id();
	public static final String CERIF_RELATIONSHIPS_UUID = Relationship.class.getAnnotation( CERIFClassScheme.class ).id();

	public static final String CERIF_DATAMODEL_FACTS_UUID = "2a29befc-305f-405a-b808-9ed0dc6c61ff";
	
	public static final String CERIF_DMF_APPLICABLE_UUID = "f22733fc-40c8-4a28-9071-6b49bd921621";
	public static final String CERIF_DMF_HAS_ATTRIBUTE_UUID = "836509cb-9d07-4c93-9db1-1097edc89115";
	public static final String CERIF_DMF_PK_UUID = "f98c9c26-b41e-4d3c-a958-47c6f1a47eca";
	public static final String CERIF_DMF_REFERENCE_UUID = "2eb07fe9-3910-430e-be50-232589608bf4";
	public static final String CERIF_DMF_REALIZE_UUID = "e6c43969-4aed-44ce-b2f0-7d85bbd2e898";
	public static final String CERIF_DMF_PARENT_ENTITY_UUID = "806bf389-971a-4062-bacc-adc950ec9183";
	public static final String CERIF_DMF_CHILD_ENTITY_UUID = "37f31879-a035-4fb2-8082-4dcd1980e681";
	
	private static final String CERIF_IDENTIFIER_TYPES_UUID = "bccb3266-689d-4740-a039-c96594b4d916";
	private static final String CERIF_IDENTIFIER_PHYSICAL_UUID = "4da60ca4-3480-40f1-b376-f43808b71d66";

	private static Logger LOG = Logger.getLogger( TOAD2CERIF.class.getName() );
	
	private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static Transformer transformer;

	static {
		try {
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		}
		catch (Exception ex){
			System.err.println("Error initializing the XML transformer");
		}
	}
	
	
	public static void main(final String argv[]) {
		try {

			final CommandLineParser parser = new DefaultParser();
	    	final Options options = new Options();
	    	options.addOption("f", "file", true, "full path to the TOAD file");
	    	options.addOption("o", "output", true, "full path to the folder where the generated XMLs shall be placed");
	    	options.addOption("h", "help", true, "this help message");
	    	
	    	final CommandLine line = parser.parse(options, argv);
	    	
	    	if (line.hasOption("h") || !(line.hasOption("f") && line.hasOption("o")))
	    	{
	    		HelpFormatter formatter = new HelpFormatter();
	    		formatter.printHelp( "toad2cerif", options );
	    		// print out the help
	    		System.exit(line.hasOption("h")?0:1);
	    	}
	    	
	    	final File outputFolder = new File(line.getOptionValue('o'));
	    	if (outputFolder.exists() && !outputFolder.isDirectory())
	    	{
	    		System.out.println(line.getOptionObject('o') + "is not a folder");
	    		// print out the help
	    		System.exit(1);
	    	}
	    	
	    	if (!outputFolder.exists()) {
	    		outputFolder.mkdirs();
	    	}
	    	
			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			final File cerifDMFFile = new File( outputFolder, "CERIF_Data_Model_Facts--"+CERIF_DATAMODEL_FACTS_UUID+".xml" );
	    	final Document cerifDMFDoc = dBuilder.parse( cerifDMFFile );
	    	final Element cerifDMFSchemeEl = XMLUtils.getSingleElement( cerifDMFDoc.getDocumentElement(), "cfClassScheme" );
	    	final Element cerifDMFApplicableCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_APPLICABLE_UUID );
	    	final Element cerifDMFHasAttributeCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_HAS_ATTRIBUTE_UUID );
	    	final Element cerifDMFPKCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_PK_UUID );
	    	final Element cerifDMFReferenceCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_REFERENCE_UUID );
	    	
	    	final Element cerifDMFRealizeCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_REALIZE_UUID );
	    	final Element cerifDMFParentEntityCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_PARENT_ENTITY_UUID );
	    	final Element cerifDMFChildEntityCfClassEl = findByCfClassId( cerifDMFSchemeEl, CERIF_DMF_CHILD_ENTITY_UUID );
	    	
	    	final File cerifFedIdFile = new File( outputFolder, "Identifier_Types--"+CERIF_IDENTIFIER_TYPES_UUID+".xml" );
	    	final Document cerifFedIdDoc = dBuilder.parse( cerifFedIdFile );
	    	final Element cerifFedIdSchemeEl = XMLUtils.getSingleElement( cerifFedIdDoc.getDocumentElement(), "cfClassScheme" );
	    	final Element cerifFedIdPhysicalModel = findByCfClassId( cerifFedIdSchemeEl, CERIF_IDENTIFIER_PHYSICAL_UUID );

	    	// read in the model
	    	final File fXmlFile = new File(line.getOptionValue('f'));
			final ToadModelParser modelParser = new ToadModelParser();
			final Model model = modelParser.readInModel( fXmlFile );

			// now dump the data structures into the XML files
			final Document attributesXML = dBuilder.newDocument();
			final Element attributesRootEl = createCERIFDocumentElement(fXmlFile, model.getModifiedDate(), attributesXML);
			final Element attributesSchemeEl = createCfClassSchemeElement(attributesRootEl, CERIF_ATTRIBUTES_UUID, "CERIF Attributes",
					"This scheme contains all the available attributes of the CERIF Entities");

			final Document entitiesXML = dBuilder.newDocument();
			final Element entitiesRootEl = createCERIFDocumentElement(fXmlFile, model.getModifiedDate(), entitiesXML);
			final Element entitiesSchemeEl = createCfClassSchemeElement(entitiesRootEl, CERIF_ENTITIES_UUID, "CERIF Entities",
					"This scheme contains defined CERIF concepts such as person, organisation, research infrastructure (being not only a 1:1 representation of the CERIF entities), but even more, e.g. research infrastructure subsumes facilty, equipment and service and output subsumes publication, patent, and product in CERIF.");
			
			final Document relationshipXML = dBuilder.newDocument();
			final Element relationshipRootEl = createCERIFDocumentElement(fXmlFile, model.getModifiedDate(), relationshipXML);
			final Element relationshipSchemeEl = createCfClassSchemeElement(relationshipRootEl, CERIF_RELATIONSHIPS_UUID, "CERIF Relationships",
					"This scheme contains the details about the realization of the relationship between CERIF Entities");
			
			final Document entityTypesXML = dBuilder.newDocument();
			final Element entityTypesRootEl = createCERIFDocumentElement(fXmlFile, model.getModifiedDate(), entityTypesXML);
			final Element entityTypesSchemeEl = createCfClassSchemeElement(entityTypesRootEl, CERIF_ENTITY_TYPES_UUID, "CERIF Entity Types",
					"This scheme contains the available classification for the CERIF Entities");

			for ( final Entity entity : model.iterableEntities() ) {
				final Element entityClassEl = createCfClassElement(entitiesSchemeEl, entity.getUuid().toString(), entity.getTerm(), entity.getNotes(), entity.getComments(), entity.getLogicalName());
				createCfFedIdElement(entityClassEl, entity.getPhysicalName(), cerifFedIdPhysicalModel);
				createCfClassClass2Element(entityClassEl, entity.getEntityType(), cerifDMFApplicableCfClassEl);
				final List<Attribute> entityPkAttributes = entity.getPrimaryKey().getAttributes();
				for ( final Attribute attr : entity.getAttributes() ) {
					final String physicalName = attr.getPhysicalName();
					final Element attrCfClassEl = createCfClassElement(attributesSchemeEl, attr.getUuid().toString(), physicalName);
					createCfFedIdElement(attrCfClassEl, physicalName.replaceAll( "^.*\\.", "" ), cerifFedIdPhysicalModel);
					createCfClassClass1Element(entityClassEl, attrCfClassEl, cerifDMFHasAttributeCfClassEl);
					
					if (entityPkAttributes.contains(attr)) {
						createCfClassClass2Element(entityClassEl, attrCfClassEl, cerifDMFPKCfClassEl);
					}
				}
			}
						
			for ( final Relationship rl : model.iterableRelationships() ) {
				final Element relationClassEl = createCfClassElement(relationshipSchemeEl, rl.getUuid().toString(), rl.getName(), rl.getNotes(), rl.getComments(), null);
				createCfClassClass1Element(relationClassEl, rl.getParentEntity(), cerifDMFParentEntityCfClassEl);
				createCfClassClass1Element(relationClassEl, rl.getChildEntity(), cerifDMFChildEntityCfClassEl);
				final Iterator<Attribute> pkAttrIterator = rl.getPk().getAttributes().iterator();
				for ( final Attribute fkAttr : rl.getFk().getAttributes() ) {
					final Attribute pkAttr = pkAttrIterator.next();
					createCfClassClass1Element(relationClassEl, pkAttr, cerifDMFRealizeCfClassEl);
					createCfClassClass1Element(relationClassEl, fkAttr, cerifDMFRealizeCfClassEl);
					final Element cfClassFkChildAttr = findByCfClassId( attributesSchemeEl, fkAttr.getUuid().toString() );
					createCfClassClass1Element(cfClassFkChildAttr, pkAttr, cerifDMFReferenceCfClassEl);
				}
			}
			
			for ( final Map.Entry<UUID, String> category : model.iterableCategories() ) {
				createCfClassElement( entityTypesSchemeEl, category.getKey().toString(), category.getValue() );
			}
			
			// write the content into xml files
			writeToFile(outputFolder, "CERIF_Entities--" + CERIF_ENTITIES_UUID + ".xml", entitiesXML);
			writeToFile(outputFolder, "CERIF_Attributes--" + CERIF_ATTRIBUTES_UUID + ".xml", attributesXML);
			writeToFile(outputFolder, "CERIF_Relationships--" + CERIF_RELATIONSHIPS_UUID + ".xml", relationshipXML);
			writeToFile(outputFolder, "CERIF_Entity_Types--" + CERIF_ENTITY_TYPES_UUID+ ".xml", entityTypesXML);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Element findByCfClassId( Element cfClassSchemeEl, String cfClassIdSearched ) {
		final NodeList children = cfClassSchemeEl.getChildNodes();
		final int n = children.getLength();
		for ( int i = 0; i < n; ++i ) {
			final Node child = children.item( i );
			if ( child instanceof Element ) {
				final Element cfClassEl = (Element) child;
				final String cfClassId = XMLUtils.getElementValue( cfClassEl, "cfClassId" );
				if ( cfClassIdSearched.equals( cfClassId ) ) {
					return cfClassEl;
				}
			}
		}
		return null;
	}

	private static Element createCfFedIdElement(Element parentEl, String identifier, Element identifierType) {
		Element cfFedEl = createSubElement(parentEl, "cfFedId");
		Element cfFedIdEl = createSubElement(cfFedEl, "cfFedId", identifier, null);
		addCfClassReference( cfFedEl, identifierType, "");
		return cfFedIdEl;
	}
	
	private static Element createCfClassClass1Element(Element parentEl, Object cfClass2El, Element roleCfClassEl) {
		if ( cfClass2El != null ) {
			Element cfClassClassEl = createSubElement(parentEl, "cfClass_Class");
			addCfClassReference( cfClassClassEl, cfClass2El, "2" );
			addCfClassReference( cfClassClassEl, roleCfClassEl, "" );
			return cfClassClassEl;
		} else {
			return null;
		}
	}

	private static Element createCfClassClass2Element(Element parentEl, Object cfClass1, Element roleCfClassEl ) {
		if ( cfClass1 != null ) {
			Element cfClassClassEl = createSubElement(parentEl, "cfClass_Class");
			addCfClassReference( cfClassClassEl, cfClass1, "1" );
			addCfClassReference( cfClassClassEl, roleCfClassEl, "" );
			return cfClassClassEl;
		} else {
			return null;
		}
	}

	private static void addCfClassReference(Element parentEl, Object cfClass, String idSuffix) {
		if ( cfClass instanceof Element ) {
			final Element cfClassEl = (Element) cfClass;
			Document doc = parentEl.getOwnerDocument();
			parentEl.appendChild( XMLUtils.cloneElementAs( XMLUtils.getSingleElement( cfClassEl, "cfClassId" ), doc, "cfClassId" + idSuffix ) );
			Element cfClassSchemeEl = (Element) cfClassEl.getParentNode();
			parentEl.appendChild( XMLUtils.cloneElementAs( XMLUtils.getSingleElement( cfClassSchemeEl, "cfClassSchemeId" ), doc, "cfClassSchemeId" + idSuffix ) );			
		} else if ( cfClass != null ) {
			final Class<? extends Object> class0 = cfClass.getClass();
			Class<? extends Object> class1 = class0;
			CERIFClassScheme annotation = null;
			while ( class1 != null && ( annotation = class1.getAnnotation( CERIFClassScheme.class ) ) == null ) {
				class1 = class1.getSuperclass();
			}
			if ( annotation != null ) {
				try {
					final Method getUuidMethod = class1.getMethod( "getUuid" );
					final String cfClassId = getUuidMethod.invoke( cfClass ).toString();
					final Method getTermMethod = class1.getMethod( "getTerm" );
					final String cfTerm = (String) getTermMethod.invoke( cfClass );
					createSubElement( parentEl, "cfClassId" + idSuffix, cfClassId, cfTerm );
					createSubElement( parentEl, "cfClassSchemeId" + idSuffix, annotation.id(), annotation.name() );
				} catch ( final Exception e ) {
					throw new IllegalArgumentException( "Cannot process cfClass of type " + class0.getName(), e );
				}
			} else {
				throw new IllegalArgumentException( "Cannot process cfClass of type " + class0.getName() + ": no CERIFClassScheme annotation present" );
			}			
		}
	}
	
	private static Element createCfClassElement(Element parentEl, String cfClassId, String cfTerm) {
		return createCfClassElement(parentEl, cfClassId, cfTerm, null, null);
	}
	
	private static Element createCfClassElement(Element parentEl, String cfClassId, String cfTerm, String cfDesc, String cfDef) {
		return createCfClassElement(parentEl, cfClassId, cfTerm, cfDesc, cfDef, null);
	}
	
	private static Element createCfClassElement(Element parentEl, String cfClassId, String cfTerm, String cfDesc, String cfDef, String physicalName) {
		Element cfClassEl = createSubElement(parentEl, "cfClass");
		createSubElement(cfClassEl, "cfClassId", cfClassId, cfTerm);	
		addLangTrans(createSubElement(cfClassEl, "cfTerm", cfTerm));	
		addLangTrans(createSubElement(cfClassEl, "cfTermSrc", "CERIF ER-M"));
		
		if (cfDesc != null) {
			addLangTrans(createSubElement(cfClassEl, "cfDescr", ToadModelParser.cleanText(cfDesc)));
			addLangTrans(createSubElement(cfClassEl, "cfDescrSrc", ToadModelParser.extractInfo("source", cfDesc, "CERIF Task Group")));
		}
		if (cfDef != null) {
			addLangTrans(createSubElement(cfClassEl, "cfDef", ToadModelParser.cleanText(cfDef)));
			addLangTrans(createSubElement(cfClassEl, "cfDefSrc", ToadModelParser.extractInfo("source", cfDef, "CERIF Task Group")));
		}
		return cfClassEl;
	}

	private static Element createCfClassSchemeElement(Element parentEl, String uuid, String name, String description ) {
		Element cfClassSchemaAttr = createSubElement(parentEl, "cfClassScheme");
		createSubElement(cfClassSchemaAttr, "cfClassSchemeId", uuid, name);
		addLangTrans(createSubElement(cfClassSchemaAttr, "cfName", name));
		addLangTrans(createSubElement(cfClassSchemaAttr, "cfDescr", description));
		return cfClassSchemaAttr;
	}

	private static void writeToFile(File outputFolder, String filename, Document entityTypesXML) throws TransformerException {
		DOMSource source = new DOMSource(entityTypesXML);
		final File file = new File(outputFolder, filename);
		StreamResult result = new StreamResult(
				file);
		transformer.transform(source, result);
		LOG.info( filename + ": " + file.length() + "B" );
	}

	private static Element createCERIFDocumentElement(File fXmlFile, String modifiedDate, Document doc) {
		Element cerifElement = doc.createElement("CERIF");
		cerifElement.setAttribute("xmlns", "urn:xmlns:org:eurocris:cerif-1.6.1-3");
		cerifElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		cerifElement.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation", "urn:xmlns:org:eurocris:cerif-1.6.1-3 http://www.eurocris.org/Uploads/Web%20pages/CERIF-1.6.1/CERIF_1.6.1_3.xsd");
		cerifElement.setAttribute("date", modifiedDate.substring(0, 10));
		cerifElement.setAttribute("sourceDatabase", fXmlFile.getName());
		return (Element) doc.appendChild(cerifElement);
	}

	private static void addLangTrans(Element element) {
		element.setAttribute("cfLangCode", "en");
		element.setAttribute("cfTrans", "o");
	}

	private static Element createSubElement(Element parentEl, String tagName ) {
		return createSubElement(parentEl, tagName, null);
	}
	
	private static Element createSubElement(Element parentEl, String tagName, String textContent ) {
		return createSubElement(parentEl, tagName, textContent, null);
	}
	
	private static Element createSubElement(Element parentEl, String tagName, String textContent, String comment ) {
		Document doc = parentEl.getOwnerDocument();
		Element sub = doc.createElement(tagName);
		if (textContent != null) {
			sub.setTextContent(textContent);
			if (comment != null) {
				sub.appendChild( doc.createComment( " " + comment + " " ) );
			}
		}
		return (Element) parentEl.appendChild(sub);
	}

}