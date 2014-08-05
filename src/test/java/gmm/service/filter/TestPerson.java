package gmm.service.filter;

public class TestPerson {
	
	private String name;
	private Integer age;
	private String hometown;
	private String sex;
	
	public TestPerson(String name, Integer age, String hometown, String sex) {
		this.name = name;
		this.age = age;
		this.hometown = hometown;
		this.sex = sex;
	}

	public String getName() {
		return name;
	}

	public Integer getAge() {
		return age;
	}

	public String getHometown() {
		return hometown;
	}

	public String getSex() {
		return sex;
	}
	
	@Override
	public String toString() {
		return "Person [name="+name+" age="+age+" hometown="+hometown+" sex="+sex+"]";
	}
}
