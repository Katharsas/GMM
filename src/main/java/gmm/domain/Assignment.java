package gmm.domain;

/**
 * Association class, assigns A to B
 */
public class Assignment<A,B> {
	
	final private A a;
	final private B b;
	
	public Assignment(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	public A getAssigned() {
		return a;
	}
	public B getReference() {
		return b;
	}
}
