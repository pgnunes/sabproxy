package com.pnunes.sabproxy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.*;

public class HitCounter {
    ConcurrentMap<String, Integer> hitCounter = null;

    public HitCounter(){
        hitCounter = new ConcurrentHashMap<String, Integer>();
    }

    public void addHit(String hitString){
        int currHits = 0;
        try{
            currHits = hitCounter.get(hitString);
        }catch(Exception e){
        }

        if(currHits == 0){
            hitCounter.put(hitString, 1);
        }else {
            hitCounter.put(hitString, currHits + 1);
        }
    }

    public Map<String, Integer> getTopHits(){
        Map<String, Integer> testMap = new HashMap(hitCounter);

        return Utils.sortByValue(testMap);

    }
}
