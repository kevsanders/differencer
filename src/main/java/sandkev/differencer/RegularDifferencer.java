package sandkev.differencer;

import sandkev.differencer.api.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Supplier;


public class RegularDifferencer<T extends Identifiable<K>,K,C extends Comparator<T>,D extends DiffComparator<T>,I extends Iterable<T>>
        implements DiffAlgorithm<T,K,C,D,I> {

    private final C keyComparator;
    private final D dataComparator;
    public RegularDifferencer(C keyComparator, D dataComparator){
        this.keyComparator = keyComparator;
        this.dataComparator = dataComparator;
    }

    @Override
    public void computeDiff(Supplier<I> expectedSource, Supplier<I> actualSource, ComparisonResultHandler<T,K> handler) {

        Iterable<T> expectedIter = expectedSource.get();
        Iterable<T> actualIter = actualSource.get();

        Iterator<T> expectedItor = expectedIter.iterator();
        Iterator<T> actualItor = actualIter.iterator();

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

            direction = checkDirection(actual, previousActual, direction);
            direction = checkDirection(expected, previousExpected, direction);
            if(direction==0 && previousExpected!=null){
                System.out.println("dup key: " + expected.getId());
            }

            int keyMatch = keyComparator.compare(actual, expected);
            if(keyMatch == 0){
                //same key
                DiffSummary diff = dataComparator.compare(actual,expected);
                if( diff.getComparisonResult() == ComparisonResult.Equal ) {
                    handler.onEqual(expected.getId());
                } else if(diff.getComparisonResult() == ComparisonResult.ApproximatelyEqual ) {
                    handler.onApproximatelyEqual(expected.getId(), diff);
                } else {
                    handler.onChanged(expected.getId(), diff);
                }
            }else if( keyMatch > 0){
                //actual is bigger
                if(direction<0){
                    handler.onDropped(expected.getId(), expected);
                }else {
                    handler.onAdded(actual.getId(), actual);
                }
            }else {
                //actual is smaller
                if(direction<0){
                    handler.onAdded(actual.getId(), actual);
                }else {
                    handler.onDropped(expected.getId(), expected);
                }
            }

            previousActual = actual;
            previousExpected = expected;
            if( keyMatch==0 ) {
                //increment both sides
                actual = nextOrNull(actualItor);
                expected = nextOrNull(expectedItor);
                direction = checkDirection(actual, previousActual, direction);
                direction = checkDirection(expected, previousExpected, direction);
            } else if( keyMatch > 0 ) {
                //actual is bigger
                if(direction<0) {
                    expected = nextOrNull(expectedItor);
                    direction = checkDirection(expected, previousExpected, direction);
                }else {
                    actual = nextOrNull(actualItor);
                    direction = checkDirection(actual, previousActual, direction);
                }
            }else {
                //actual is smaller
                if(direction<0) {
                    actual = nextOrNull(actualItor);
                    direction = checkDirection(actual, previousActual, direction);
                }else {
                    expected = nextOrNull(expectedItor);
                    direction = checkDirection(expected, previousExpected, direction);
                }
            }

            if(expected==null && actual!=null){
                handler.onAdded(actual.getId(), actual);
            }
            if(actual==null && expected!=null){
                handler.onDropped(expected.getId(), expected);
            }

        }


    }

    private int checkDirection(T item, T previousItem, int currentDirection) {
        if(item==null||previousItem==null){
            return currentDirection;
        }
        int direction=keyComparator.compare(previousItem, item);
        if(direction==0){
            throw new IllegalArgumentException("Duplicate found in primary key: " + item.getId());
        }
        if(currentDirection != 0 && direction != 0){
            if(Math.signum(currentDirection)!=Math.signum(direction)){
                throw new IllegalArgumentException("keys (" + previousItem.getId() + "->" + item.getId() + ") going in the wrong direction expected " + Math.signum(currentDirection) + " but was " + Math.signum(direction));
            }
        }
        return direction;
    }

    private T nextOrNull(Iterator<T> itor) {
        return itor==null||!itor.hasNext()?null:itor.next();
    }

}
