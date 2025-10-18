package com.mytry.editortry.Try.utils.cache;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.mytry.editortry.Try.utils.cache.components.CacheSuggestionOuterProjectFile;
import com.mytry.editortry.Try.utils.parser.CodeAnalysisUtils;
import com.mytry.editortry.Try.utils.parser.DedicatedJavaParser;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


// данный компонент загружает/формирует кеш стандартной библиотеки java, пользуясь архивом src.zip
// группировка данных происходит на первой букве класса
// для интереса можно реализовать что-то вроде статистики вызовов
@Component
public class JavaStandartLibraryCache {
    @Value("${common.directory}")
    private String project_commons_path;



    private final String basicPath = "java.base/java/";
    private final List<String> zipStructurePaths = List.of(
            basicPath +"io",
            basicPath +"lang",
            basicPath +"math",
            basicPath +"net",
            basicPath +"nio",
            basicPath +"security",
            basicPath +"text",
            basicPath +"time",
            basicPath +"util"

    );


    private HashMap<String, List<CacheSuggestionOuterProjectFile>> cache = new HashMap<>();





    // возвращаем список типов, соответствующих введенному пользователем фрагменту
    public List<CacheSuggestionOuterProjectFile> getTypesByFragment(String fragment){

        String firstLetter = fragment.substring(0,1);

        List<CacheSuggestionOuterProjectFile> list=cache.get(firstLetter);

        if (list == null){
            return List.of();
        }
        else {
            return list.stream().filter(el->el.getName().startsWith(fragment)).toList();
        }
    }



    // load serialized cache or generate new cache
    @PostConstruct
    public void initCache() throws Exception {






        String serPath = project_commons_path+"standart_cache.bat";

        if (Files.exists(Path.of(serPath))){
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(serPath))) {
                HashMap<String, List<CacheSuggestionOuterProjectFile>> read =
                        (HashMap<String, List<CacheSuggestionOuterProjectFile>>) in.readObject();
                cache = read;

                //System.out.println(read);
                //System.out.println("serialization result");
                return;

            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        /*
        если произошла ошибка или файл отсутствует, мы перезаписываем и сохраняем кеш
         */

        String javaHome = System.getProperty("java.home");
        System.out.println(javaHome);

        String way = javaHome+"/lib/src.zip";

        String alternateWay = javaHome+"/src.zip";

        String workingWay = null;



        if (Files.exists(Path.of(way))){
            workingWay = way;
        }
        else if (Files.exists(Path.of(alternateWay))){
            workingWay = alternateWay;
        }

        else {
            throw new IllegalArgumentException("invalid java home path. Standart library completions doesn't work here");
        }

        try(ZipFile zip = new ZipFile(workingWay)){

            Enumeration<? extends ZipEntry> entries = zip.entries();

            List<ZipEntry> files = new ArrayList<>();
            while (entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()){
                    for (String s:zipStructurePaths){
                        if (entry.getName().startsWith(s) && !entry.getName().endsWith("package-info.java")){
                            files.add(entry);

                        }
                    }
                }




            }

            files.forEach(f->{
                try(InputStream input = zip.getInputStream(f)) {
                    byte[] content = input.readAllBytes();
                    String code = new String(content, StandardCharsets.UTF_8);

                    CompilationUnit compilationResult = DedicatedJavaParser.getInstance()
                            .parse(code).getResult().orElseThrow();

                    CacheSuggestionOuterProjectFile typeApi = CodeAnalysisUtils.generateOuterFileApi(compilationResult);
                    if(typeApi.getName()!=null){

                        String firstLetter = typeApi.getName().substring(0,1);
                        List<CacheSuggestionOuterProjectFile> list = cache
                                .computeIfAbsent(firstLetter, (k)->new ArrayList<>());
                        list.add(typeApi);


                    }




                }

                catch (Exception e){
                    e.printStackTrace();
                }
            });

        }
        catch (Exception e){
            throw new IllegalArgumentException("error while creating standart library cache");
        }

        // сериализуем кеш
        serializeCache();
    }

    private void serializeCache() throws Exception{
        String path = project_commons_path+"standart_cache.bat";
        System.out.println(path);




        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {

            out.writeObject(cache);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


}
