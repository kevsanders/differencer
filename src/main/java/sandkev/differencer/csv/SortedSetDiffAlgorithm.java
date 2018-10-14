package sandkev.differencer.csv;

import sandkev.differencer.NaturallyKeyed;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;

/**
 * Created by kevin on 08/10/2018.
 */
public class SortedSetDiffAlgorithm<T extends NaturallyKeyed<K>, K extends Serializable, C extends Comparator<K>, D extends Comparator<T>, I extends SortedSet<T>>
 implements DiffAlgorithm<T, K, C ,D, I>{

    double tollerence = 0;
    C keyComparator;
    D dataComparator;

    public SortedSetDiffAlgorithm(C keyComparator, D dataComparator) {
        this.keyComparator = keyComparator;
        this.dataComparator = dataComparator;
    }

/*
    public void computeDiff(SortedSet<T> original, SortedSet<T> revision, ComparisonResultHandler handler) {
        validateComparitors(original, revision);
        computeDiff(original, revision, handler);
    }

    private void validateComparitors(SortedSet<T> original, SortedSet<T> revision) {
        Comparator originalComparitor = original.comparator();
        Comparator revisionComparitor = original.comparator();
        if(originalComparitor != null && !originalComparitor.equals(revisionComparitor)){
            throw new IllegalArgumentException("Comparitors for the two sets must be the same");
        }
    }
*/


    public void computeDiff(I originalIterable, I revisionIterable, ComparisonResultHandler handler) {
    //public void computeDiff(Iterable<T> originalIterable, Iterable<T> revisionIterable, ComparisonResultHandler handler) {

        Iterator<T> originalIterator = originalIterable.iterator();
        Optional<T> originalOptional = next(originalIterator);
        if(!originalOptional.isPresent()){
            throw new IllegalArgumentException("Stream of expected data is empty");
        }

        Iterator<T> revisionIterator = revisionIterable.iterator();
        Optional<T> revisionOptional = next(revisionIterator);
        if(!revisionOptional.isPresent()) {
            throw new IllegalArgumentException("Stream of actual data is empty");
        }

        T previousOriginal = null;
        T previousRevision = null;
        int direction = 0;
        while ( originalOptional.isPresent() || revisionOptional.isPresent()){ //originalOptional.isPresent() && revisionOptional.isPresent()

            T orginal = originalOptional.orElse(null);
            T revision = revisionOptional.orElse(null);

            Optional<K> orginalNaturalKey = Optional.ofNullable(orginal==null?null:orginal.getNaturalKey());
            direction = checkDirection(orginal, previousOriginal, direction);
            direction = checkDirection(revision, previousRevision, direction);

            Optional<K> revisionNaturalKey = Optional.ofNullable(revision==null?null:revision.getNaturalKey());
            int keyMatch = keyComparator.compare(orginalNaturalKey.orElse(null), revisionNaturalKey.orElse(null));
            if(keyMatch == 0){
                //same key
                int match = dataComparator.compare(orginal, revision);
                if(match==0){
                    handler.onEqual(orginalNaturalKey, orginal, revision, "");
                }else {
                    if(tollerence > 0){
                        if(approximatelyEquals(orginal, revision, tollerence)){
                            handler.onApproximatelyEqual(orginalNaturalKey, orginal, revision, "");
                        }else {
                            handler.onChanged(orginalNaturalKey, orginal, revision, "");
                        }
                    }else {
                        handler.onChanged(orginalNaturalKey, orginal, revision, "");
                    }
                }
                originalOptional = next(originalIterator);
                revisionOptional = next(revisionIterator);

            }else if( keyMatch > 0){
                //original is bigger
                handler.onInserted(revisionNaturalKey, revision, "");
                revisionOptional = next(revisionIterator);
            }else {
                //revision is bigger
                handler.onDeleted(orginalNaturalKey, orginal, "");
                originalOptional = next(originalIterator);
            }

            previousOriginal = orginal;
            previousRevision = revision;

        }

    }

    private int checkDirection(T item, T previousItem, int currentDirection) {
        if(item==null||previousItem==null||item==previousItem){
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

    private boolean approximatelyEquals(T orginal, T revision, double tollerence) {
        return false;
    }

    private Optional<T> next(Iterator<T> itor) {
        return itor==null||!itor.hasNext()?Optional.<T>empty():Optional.ofNullable(itor.next());
    }


}
