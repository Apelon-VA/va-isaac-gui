
package gov.va.isaac.models.cem;

import gov.va.isaac.AppContext;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.model.constants.InformationModelsConstants;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a CEM constraint.
 */
public class CEMConstraint {
  private static final Logger LOGGER = LoggerFactory.getLogger(OTFUtility.class);

  /**  The path. */
  private String path;

  /**  The value. */
  private String value;

  /**
   * Instantiates a {@link CEMConstraint}.
   */
  public CEMConstraint() {
    // do nothing
  }

  /**
   * Returns the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path.
   *
   * @param path the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Returns the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  
  /**
   * Returns the ceml tag name.
   *
   * @return the ceml tag name
   */
  public static String getCemlTagName() {
    return "constraint";
  }
  
  /**
   * Sets the value.
   *
   * @param value the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((path == null) ? 0 : path.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CEMConstraint other = (CEMConstraint) obj;
    if (path == null) {
      if (other.path != null)
        return false;
    } else if (!path.equals(other.path))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }
  
	public void handleConstraintEnumerationRefex() {
		if (value.endsWith("_VALUESET_CODE")) {
			if (!valueSetExists()) {
				try {
					// Create Enumeration
					AppContext.getRuntimeGlobals().disableAllCommitListeners();
					DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(value, value, "Value Set Sememe for " + value, new DynamicSememeColumnInfo[] {},
							InformationModelsConstants.CEM_ENUMERATIONS.getNid(), null, null);
				} catch (RuntimeException e) {
					LOGGER.error("Unable to create CEM enumeration for " + value);
				}
				finally
				{
					AppContext.getRuntimeGlobals().enableAllCommitListeners();
				}
			}
		}
	}
	
	private boolean valueSetExists() {
			// Get UUID
			UUID uuid = OTFUtility.getUuidForFsn(value, value);
//			UUID uuid = UuidT5Generator.get(UUID PATH_ID_FROM_FS_DESC, value);
	
			return Get.identifierService().hasUuid(uuid);
	}
}
