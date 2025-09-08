package com.mytry.editortry.Try.utils.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// система напрямую связана с websocket
// происходит первая  подписка на файл или проект в целом = создается кеш проекта
@Component
public class CacheSystem {

    // java standart library cache
    @Autowired
    private JavaStandartLibraryCache standartLibraryCache;

    // пара project_id : project cache
    private final Map<Long, ProjectCache> projectsCaches = new ConcurrentHashMap<>();


    // пара sessionId - id проекта
    private final Map<String, Long> subscribersProjectsAssociation = new ConcurrentHashMap<>();


    // если кеш отсутствует, то имеет место баг, т.к. метод вызывается активным подписчиком
    public ProjectCache getProjectCache(Long id){

        ProjectCache cache = projectsCaches.get(id);

        if (cache == null) {
            throw new IllegalStateException("Кеш проекта не был создан подписчиком");
        }


        return cache;

    }


    // утилитарный метод ля поиска типа по фрагменту имени
    public List<CacheSuggestionOuterProjectType> getStandartLibraryTypesByFragment(String fragment){
        return standartLibraryCache.getTypesByFragment(fragment);
    }


    // мы фиксируем момент последнего изменения кеша для того, чтобы специальный обработчик чистил зависшие старые кеши
    public void setProjectChange(Long projectId){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache!=null){
            projectCache.notifyUpdate();
        }
    }

    // обновляем кеш проекта
    public  void updateProjectCache(Long projectId,
                                                Map<String, List<CacheSuggestionInnerProjectFile>> packageToFileAssociation,
                                                Map<Long, CacheSuggestionInnerProjectFile> idToFileAssociation){

        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache!=null){
            projectCache.setIdToFileAssociation(idToFileAssociation);
            projectCache.setPackageToFileAssociation(packageToFileAssociation);
        }

    }



    public Map<String, List<CacheSuggestionInnerProjectFile>> getProjectCacheState(Long projectId){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache!=null && !projectCache.isEmpty()){
            return projectCache.getPackageToFileAssociation();
        }
        return null;
    }

    // метод вызывается каждый раз, когда сохраняется файл. Помним, что работаем с двумя ассоциациями сразу,
    // поэтому разумнее не перезаписывать объект вообще
    public void updateFileCache(Long projectId, Long fileId, CacheSuggestionInnerProjectFile type){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache != null){
            projectCache.updateFileCache(fileId, type);
        }
    }


    // уведомляем систему о том, что в проекте произошли значительные изменения, делающие кеш неактуальным
    public void clearProjectCacheContent(Long projectId){
        ProjectCache projectCache = projectsCaches.get(projectId);
        if (projectCache!=null){
            // чистим файловый контент, но не трогаем трекер подписчиков и прочую инфу
            projectCache.clearExpiredCache();
        }
    }

    // полное стирание кеша - когда проект не имеет подписчиков
    public void removeProjectCache(Long projectId){
        projectsCaches.remove(projectId);
    }








    public void addProjectSubscription(String sessionId, Long projectId){


        ProjectCache projectCache = projectsCaches.computeIfAbsent(projectId, k -> new ProjectCache());
        subscribersProjectsAssociation.put(sessionId, projectId);


        projectCache.addSubscriber(sessionId);


    }


    // удаляем подписчика и, если он последний, кеш
    public void removeProjectSubscription(String sessionId){

        Long projectId = subscribersProjectsAssociation.remove(sessionId);


        ProjectCache cache = projectsCaches.get(projectId);

        if (cache!=null){
            int amount = cache.removeSubscriber(sessionId);
            if (amount==0){
                projectsCaches.remove(projectId); // очищаем кеш проекта вместе с файлами
            }

        }

    }


    public Map<Long, ProjectCache> getProjectsCaches() {
        return projectsCaches;
    }
}
