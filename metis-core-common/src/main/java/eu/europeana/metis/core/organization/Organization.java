/*
 * Copyright 2007-2013 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.metis.core.organization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import eu.europeana.metis.core.common.AltLabel;
import eu.europeana.metis.core.common.Country;
import eu.europeana.metis.core.common.Domain;
import eu.europeana.metis.core.common.GeographicLevel;
import eu.europeana.metis.core.common.HarvestingMetadata;
import eu.europeana.metis.core.common.Language;
import eu.europeana.metis.core.common.PrefLabel;
import eu.europeana.metis.core.common.Role;
import eu.europeana.metis.core.common.Scope;
import eu.europeana.metis.core.common.Sector;
import eu.europeana.metis.core.dataset.Dataset;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

/**
 * The Organization representation in METIS
 * Created by ymamakis on 2/17/16.
 */
@XmlRootElement
@Entity
public class Organization {
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String organizationId;

    /**
     * The organization URI from the CRM
     */
    private String organizationUri;
    /**
     * The harvesting metadata that are applicable for all the datasets of the organization
     * Override that to specify dataset specific ones
     */
    private HarvestingMetadata harvestingMetadata;

    private String name;

    private Date created;

    private Date modified;

    private String acronym;

    @Indexed
    @JacksonXmlProperty(localName = "roles")
    private List<Role> roles;

    /**
     * The datasets associated with the organization
     */
    @Reference
    private List<Dataset> datasets;

    private String createdByLdapId;

    private String updatedByLdapId;

    @Embedded
    @JacksonXmlProperty(localName = "prefLabels")
    private List<PrefLabel> prefLabel;

    @Embedded
    @JacksonXmlProperty(localName = "altLabels")
    private List<AltLabel> altLabel;

    @JacksonXmlProperty(localName = "sameAsList")
    private String[] sameAs;

    private String description;

    private String logoLocation;

    private Domain domain;

    private Sector sector;

    private GeographicLevel geographicLevel;

    private String website;

    @Indexed
    private Country country;

    private Language language;

    private Scope scope;

    private boolean dea;

    @Indexed
    private boolean optInIIIF;

    @XmlElement
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @XmlElement
    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    @XmlElement
    public String getOrganizationUri() {
        return organizationUri;
    }

    public void setOrganizationUri(String organizationUri) {
        this.organizationUri = organizationUri;
    }

    @XmlElement
    public HarvestingMetadata getHarvestingMetadata() {
        return harvestingMetadata;
    }

    public void setHarvestingMetadata(HarvestingMetadata harvestingMetadata) {
        this.harvestingMetadata = harvestingMetadata;
    }

    @XmlElement
    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @XmlElement
    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    @XmlElement
    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    @XmlElement
    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    @XmlElement
    public String getCreatedByLdapId() {
        return createdByLdapId;
    }

    public void setCreatedByLdapId(String createdByLdapId) {
        this.createdByLdapId = createdByLdapId;
    }

    @XmlElement
    public String getUpdatedByLdapId() {
        return updatedByLdapId;
    }

    public void setUpdatedByLdapId(String updatedByLdapId) {
        this.updatedByLdapId = updatedByLdapId;
    }

    @XmlElement
    public List<PrefLabel> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(List<PrefLabel> prefLabel) {
        this.prefLabel = prefLabel;
    }

    @XmlElement
    public List<AltLabel> getAltLabel() {
        return altLabel;
    }

    public void setAltLabel(List<AltLabel> altLabel) {
        this.altLabel = altLabel;
    }

    @XmlElement
    public String[] getSameAs() {
        return sameAs;
    }

    public void setSameAs(String[] sameAs) {
        this.sameAs = sameAs;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement
    public String getLogoLocation() {
        return logoLocation;
    }

    public void setLogoLocation(String logoLocation) {
        this.logoLocation = logoLocation;
    }

    @XmlElement
    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    @XmlElement
    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    @XmlElement
    public GeographicLevel getGeographicLevel() {
        return geographicLevel;
    }

    public void setGeographicLevel(GeographicLevel geographicLevel) {
        this.geographicLevel = geographicLevel;
    }

    @XmlElement
    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    @XmlElement
    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    @XmlElement
    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    @XmlElement
    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }


    public void setDea(boolean dea) {
        this.dea = dea;
    }

    @XmlElement
    public boolean isDea() {
        return dea;
    }

    @XmlElement
    public boolean isOptInIIIF() {
        return optInIIIF;
    }

    public void setOptInIIIF(boolean optInIIIF) {
        this.optInIIIF = optInIIIF;
    }
}
