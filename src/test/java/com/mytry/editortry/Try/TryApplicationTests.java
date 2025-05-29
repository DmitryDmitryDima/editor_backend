package com.mytry.editortry.Try;

import com.mytry.editortry.Try.dto.lm.LMResponse;
import com.mytry.editortry.Try.dto.run.RunRequest;
import com.mytry.editortry.Try.service.CompilerService;
import com.mytry.editortry.Try.service.AIService;
import com.mytry.editortry.Try.service.ParserService;
import com.mytry.editortry.Try.utils.ai.Prompts;
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
	private AIService aiService;




	@Test
	public void testParser(){

	}

	@Test
	public void testValueAnnotation(){
		Assertions.assertEquals(aiService.getAPI_ADDRESS(), "http://127.0.0.1:11434/api/generate");
		Assertions.assertEquals(aiService.getMODEL_NAME(), "gemma3:1b");
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


			//LMResponse answer = AIService.sendARequest(Prompts.IMPORT_PROMPT +code);
			//System.out.println(answer);
		});
	}



}
