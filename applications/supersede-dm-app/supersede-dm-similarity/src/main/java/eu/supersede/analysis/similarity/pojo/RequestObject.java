/**
 * 
 */
package eu.supersede.analysis.similarity.pojo;

/**
 * @author fitsum
 *
 */


public class RequestObject {
	private Feedback feedback;
	private int k;
	private Requirement[] requirements;
	public Feedback getFeedback() {
		return feedback;
	}
	public void setFeedback(Feedback feedback) {
		this.feedback = feedback;
	}
	public int getK() {
		return k;
	}
	public void setK(int k) {
		this.k = k;
	}
	public Requirement[] getRequirements() {
		return requirements;
	}
	public void setRequirements(Requirement[] requirements) {
		this.requirements = requirements;
	}
}
