package gmm.data.ClassTests;

import static org.junit.Assert.*;
import gmm.service.data.DataConfigService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="GMM-servlet.xml")
public class DataConfigServiceTest {

	@Autowired
	DataConfigService dataConfig;
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		assertNotNull(dataConfig);
	}

}
