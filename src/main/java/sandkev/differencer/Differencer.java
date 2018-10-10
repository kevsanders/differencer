package sandkev.differencer;

import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 * Used to compare 2 streams of data
 * Data should ideally be sorted into a unique set using the same key comparator
 * eg records from a database ordered by primary key
 * eg files from a filesystem ordered by full path
 * @param <T> Type of object to be compared
 * @param <N> Natural key of object to  be compared
 * @param <K> Key comparator
 * @param <D> Data comparator
 */
public abstract class Differencer<T extends NaturallyKeyed<N>, N extends Serializable, K extends Comparator<N>, D extends Comparator<T>> {
    private enum Direction{Up,Down};
    private final K keyComparator;
    private final D dataComparator;
    public Differencer(K keyComparator, D dataComparator){
        this.keyComparator = keyComparator;
        this.dataComparator = dataComparator;
    }

    public List<Diff<T,N>> compare(Iterator<T> expectedItor, Iterator<T> actualItor){
        final List<Diff<T,N>> diffs = new ArrayList<>();
        DifferenceListener handler = new DifferenceListener<T>() {
            @Override
            public void onMatch(T expected) {
                diffs.add(new Diff(Diff.Type.Equal, expected));
            }

            @Override
            public void onDiff(T actual, T expected) {
                diffs.add(new Diff(Diff.Type.Updated, expected, actual));
            }

            @Override
            public void onMissing(T deleted) {
                diffs.add(new Diff(Diff.Type.Deleted, deleted));
            }

            @Override
            public void onAdded(T inderted) {
                diffs.add(new Diff(Diff.Type.Inserted, inderted));
            }
        };
        compare(expectedItor, actualItor, handler);
        return diffs;
    }

    public void compare(Iterator<T> expectedItor, Iterator<T> actualItor, DifferenceListener handler){

        T expected = nextOrNull(expectedItor);
        if(expected==null){
            throw new IllegalArgumentException("Stream of expected data is empty");
        }

        T actual = nextOrNull(actualItor);
        if(actual==null) {
            throw new IllegalArgumentException("Stream of actual data is empty");
        }


        T previousActual = null;
        T previousExpected = null;
        int direction = 0;
        while (actual!=null && expected!=null){

            direction = checkDirection(expected, previousExpected, direction);
            if(direction==0 && previousExpected!=null){
                System.out.println("dup key: " + expected.getNaturalKey());
            }

            int keyMatch = keyComparator.compare(actual.getNaturalKey(), expected.getNaturalKey());
            if(keyMatch == 0){
                //same key
                int match = dataComparator.compare(actual,expected);
                if(match==0){
                    handler.onMatch(expected);
                }else {
                    //TODO: apply approximatelyEqual if object can support tollerable difference
                    handler.onDiff(actual, expected);
                }
            }else if( keyMatch > 0){
                //actual is bigger
                handler.onMissing(expected);
                //expected = nextOrNull(expectedItor);
            }else {
                //actual is smaller
                handler.onAdded(actual);
                //actual = nextOrNull(actualItor);
            }

            /*
            1) we need to ensure data is sorted by primary key
               -> so we need a primaryKeyComparator
            2) we need to be able to compare the corresponding data
               -> we need and equals method that can tollerate approximatelyEqual in the case of small tolerable numberical differences

            Questions

            should we enforce the T objects to be compared to be aware of their own primaryKey (natural key)
            or is it better to provide the key separately eg in a Map.Entry key/value pair?
             */



            if(keyMatch==0){
                //increment both sides
                previousActual = actual;
                previousExpected = expected;
                actual = nextOrNull(actualItor);
                expected = nextOrNull(expectedItor);
                direction = checkDirection(actual, previousActual, direction);
                direction = checkDirection(expected, previousExpected, direction);
            }else if(keyMatch > 0){
                //actual is bigger
                previousExpected = expected;
                expected = nextOrNull(expectedItor);
                direction = checkDirection(expected, previousExpected, direction);
            }else {
                //actual is smaller
                previousActual = actual;
                direction = checkDirection(actual, previousActual, direction);
                actual = nextOrNull(actualItor);
            }

            if(expected==null && actual!=null){
                handler.onAdded(actual);
            }
            if(actual==null && expected!=null){
                handler.onMissing(expected);
            }

        }


    }

    private int checkDirection(T item, T previousItem, int currentDirection) {
        if(item==null||previousItem==null){
            return currentDirection;
        }
        int direction=keyComparator.compare(previousItem.getNaturalKey(), item.getNaturalKey());
        if(direction==0){
            System.out.println("duplicate found: " + item.getNaturalKey());
        }
        if(currentDirection != 0 && direction != 0){
            if(Math.signum(currentDirection)!=Math.signum(direction)){
                throw new IllegalArgumentException("keys (" + previousItem.getNaturalKey() + "->" + item.getNaturalKey() + ") going in the wrong direction expected " + Math.signum(currentDirection) + " but was " + Math.signum(direction));
            }
        }
        return direction;
    }

    public static class Diff<T extends NaturallyKeyed<N>, N extends Serializable>{
        enum Type{Equal,Inserted,Deleted,Updated}
        private final Type type;
        private final T item;
        private final T update;

        /*
        Questions

        I guess we don't really need to store both expected and actual
        Perhaps we should just store the Key and a list of differences
        (this could be the field that is different and the difference)

        If the data matches is it ok to just note that the key matched

        If the data is deleted how do we represent the differences?
        Is it easier to store the whole deleted object, a list of the updated fields (ie all of them) or just the key

        Similiarly if the data is inserted how do we represent the differences
        Is it easier to store the whole inserted object, a list of the updated fields (ie all of them) or just the key

         */


        public Diff(Type type, T item) {
            this(type, item, null);
            if(type==Type.Updated){
                throw new IllegalArgumentException("Missing details of updated record for key:" + item.getNaturalKey());
            }
        }
        public Diff(Type type, T item, T update) {
            this.type = type;
            this.item = item;
            this.update = update;
        }

        public Type getType() {
            return type;
        }

        public T getItem() {
            return item;
        }

        public T getUpdate() {
            return update;
        }
    }

    private T nextOrNull(Iterator<T> itor) {
        return itor==null||!itor.hasNext()?null:itor.next();
    }

}
