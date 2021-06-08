package org.openinfralabs.caerus.ndpService.model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * A XML object class for Caerus UDF (invocation metadata) information using Java native JAXB (Java Architecture for XML Binding). Example:
 * <?xml version="1.0" encoding="UTF-8"?>
 * <caerusudf id="1">
 *    <function_name>caerus-faas-spring-thumbnail</function_name>
 *    <function_inputParameters>400, 600</function_inputParameters>
 * </caerusudf>
 */
@XmlRootElement(name = "caerusudf")
@XmlAccessorType(XmlAccessType.FIELD)
public class CaerusUdf implements Serializable {

    @XmlAttribute
    private int id;
    @XmlElement (name = "function_name")
    private String function_name;
    @XmlElement (name = "function_inputParameters")
    private String function_inputParameters;

    public CaerusUdf() {}
    public CaerusUdf(int id, String function_name, String function_inputParameters) {
        super();
        this.id = id;
        this.function_name = function_name;
        this.function_inputParameters = function_inputParameters;
    }

    public int getId() {
        return id;
    }

    public String getFunctionName() {
        return function_name;
    }

    public String getFunctionInputParameters() {
        return function_inputParameters;
    }

    public void setId(int id) {
        this.id = id;
    }
    public void setFunctionName(String function_name) {
        this.function_name = function_name;
    }

    public void setFunctionInputParameters(String function_inputParameters) {
        this.function_inputParameters = function_inputParameters;
    }


    // constructor, getters and setters
}