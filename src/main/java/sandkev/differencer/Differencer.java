package sandkev.differencer;

import com.sun.corba.se.spi.ior.Identifiable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;


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
    private final K keyComparator;
    private final D dataComparator;
    public Differencer(K keyComparator, D dataComparator){
        this.keyComparator = keyComparator;
        this.dataComparator = dataComparator;
    }
    public void compare(Iterator<T> expectedItor, Iterator<T> actualItor, DifferenceListener handler){

        while (expectedItor.hasNext()){

            T expected = expectedItor.next();

            if( !actualItor.hasNext()){
                handler.onMissing(expected);
                //maybe finish here since nothing left to compare
            }else {

                T actual = actualItor.next();
                int keyMatch = keyComparator.compare(actual.getKey(), expected.getKey());
                switch (keyMatch){
                    case 0:
                        int match = dataComparator.compare(actual, expected);
                        if(match==0){
                            handler.onMatch(actual, expected);
                        }else {
                            handler.onDifference(actual, expected);
                        }
                        break;
                    default:
                        handler.onDifference(actual, expected);
                }


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



        }

    }

}
