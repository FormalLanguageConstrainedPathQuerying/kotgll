package junit.runner;

/**
 * This class defines the current version of JUnit
 */
public class Version {
	private Version() {
	}

	public static String id() {
		return "4.13.3-SNAPSHOT";
	}
	
	public static void main(String[] args) {
		System.out.println(id());
	}
}
