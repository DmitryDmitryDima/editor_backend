package com.mytry.editortry.Try;

import com.mytry.editortry.Try.dto.lm.LMResponse;
import com.mytry.editortry.Try.service.CompilerService;
import com.mytry.editortry.Try.service.AIService;
import com.mytry.editortry.Try.service.ParserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TryApplicationTests {


	@Autowired
	private CompilerService compilerService;

	@Autowired
	private ParserService parserService;

	@Autowired
	private AIService AIService;


	@Test
	public void testCompiler(){


		Assertions.assertDoesNotThrow(()->{
			String c = "class X{}";
			String answer = compilerService.makeCompilation(c);
			System.out.println(answer);
		});
	}

	@Test
	public void testParser(){

	}

	@Test
	public void testLM(){
		Assertions.assertDoesNotThrow(()->{

			/*
			String prompt = """
					
					write only imports for java code below:
					
					public class StackExample\s
					{
					    public static void main(String[] args)\s
					    {
					        List<String> hello = new ArrayList<>();
					        
					    }
					}
					
					
					""";

			*/

			String code = """
					public class StackExample\s
					{
					    public static void main(String[] args)\s
					    {
					        ArrayList<String> hello = new ArrayList<>();
					    }
					}
					""";


			LMResponse answer = AIService.sendARequest(AIService.IMPORT_PROMPT+code);
			System.out.println(answer);
		});
	}



}
