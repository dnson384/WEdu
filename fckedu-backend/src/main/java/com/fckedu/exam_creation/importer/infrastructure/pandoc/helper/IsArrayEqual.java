package com.fckedu.exam_creation.importer.infrastructure.pandoc.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IsArrayEqual {
    public static boolean execute(List<String> lst1, List<String> lst2) {
        if (lst1.size() != lst2.size()) return false;

        List<String> sorted1 = new ArrayList<String>(lst1);
        List<String> sorted2 = new ArrayList<String>(lst2);

        Collections.sort(sorted1);
        Collections.sort(sorted2);

        return sorted1.equals(sorted2);
    }
}
