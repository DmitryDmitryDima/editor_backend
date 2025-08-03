package com.mytry.editortry.Try.utils.cache;

import com.mytry.editortry.Try.model.Project;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


// система напрямую связана с websocket
// происходит первая  подписка на файл или проект в целом = создается кеш проекта
@Component
public class CacheSystem {

    // пара project_id : project cache
    private Map<Long, ProjectCache> projectsCaches;


    // пара sessionId - id проекта
    private Map<String, Long> subscribersProjectsAssosiation;







    public synchronized void addProjectSubscription(String sessionId, Long projectId){
        ProjectCache projectCache = projectsCaches.computeIfAbsent(projectId, k -> new ProjectCache());

        projectCache.addSubscriber(sessionId);

        ///
    }


    // удаляем подписчика и, если он последний, кеш
    public synchronized void removeProjectSubscription(String sessionId){

        Long projectId = subscribersProjectsAssosiation.remove(sessionId);

        ProjectCache cache = projectsCaches.get(projectId);

        if (cache!=null){
            int amount = cache.removeSubscriber(sessionId);
            if (amount==0){
                projectsCaches.remove(projectId); // очищаем кеш проекта вместе с файлами
            }

        }

    }









}
