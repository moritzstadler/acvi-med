package at.ac.meduniwien.vcfvisualize.processor.acmg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneticCode {

    private static Map<String, String> code = Stream.of(new String[][] {
            { "UUU", "Phe" },
            { "UUC", "Phe" },
            { "UUA", "Leu" },
            { "UUG", "Leu" },

            { "CUU", "Leu" },
            { "CUC", "Leu" },
            { "CUA", "Leu" },
            { "CUG", "Leu" },

            { "AUU", "Ile" },
            { "AUC", "Ile" },
            { "AUA", "Ile" },
            { "AUG", "Met" },

            { "GUU", "Val" },
            { "GUC", "Val" },
            { "GUA", "Val" },
            { "GUG", "Val" },

            { "UCU", "Ser" },
            { "UCC", "Ser" },
            { "UCA", "Ser" },
            { "UCG", "Ser" },

            { "CCU", "Pro" },
            { "CCC", "Pro" },
            { "CCA", "Pro" },
            { "CCG", "Pro" },

            { "ACU", "Thr" },
            { "ACC", "Thr" },
            { "ACA", "Thr" },
            { "ACG", "Thr" },

            { "GCU", "Ala" },
            { "GCC", "Ala" },
            { "GCA", "Ala" },
            { "GCG", "Ala" },

            { "UAU", "Tyr" },
            { "UAC", "Tyr" },
            { "UAA", "Stop" },
            { "UAG", "Stop" },

            { "CAU", "His" },
            { "CAC", "His" },
            { "CAA", "Gln" },
            { "CAG", "Gln" },

            { "AAU", "Asn" },
            { "AAC", "Asn" },
            { "AAA", "Lys" },
            { "AAG", "Lys" },

            { "GAU", "Asp" },
            { "GAC", "Asp" },
            { "GAA", "Glu" },
            { "GAG", "Glu" },

            { "UGU", "Cys" },
            { "UGC", "Cys" },
            { "UGA", "Stop" },
            { "UGG", "Stop" },

            { "CGU", "Arg" },
            { "CGC", "Arg" },
            { "CGA", "Arg" },
            { "CGG", "Arg" },

            { "AGU", "Ser" },
            { "AGC", "Ser" },
            { "AGA", "Arg" },
            { "AGG", "Arg" },

            { "GGU", "Gly" },
            { "GGC", "Gly" },
            { "GGA", "Gly" },
            { "GGG", "Gly" }
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    private static Map<String, List<String>> reverseCode;

    private static void initiateReverseMap() {
        if (reverseCode == null) {
            reverseCode = new HashMap<>();
            for (String triplet : code.keySet()) {
                String aminoAcid = code.get(triplet);
                if (!reverseCode.containsKey(aminoAcid)) {
                    reverseCode.put(aminoAcid, new LinkedList<>());
                }
                reverseCode.get(aminoAcid).add(triplet);
            }
        }
    }

    /**
     * determines the amino acid of a triplet
     *
     * @param triplet the triplet
     * @return the three letter code for the amino acid
     */
    public static String aminoAcidByTriplet(String triplet) {
        String cleanTriplet = triplet.toUpperCase();
        if (!code.containsKey(cleanTriplet)) {
            return null;
        }
        return code.get(cleanTriplet);
    }

    /**
     * determines a list of triples that may produce the given amino acid
     *
     * @param aminoAcid the amino acid
     * @return a list of triplets that produce it
     */
    public static List<String> triplesByAminoAcid(String aminoAcid) {
        initiateReverseMap();
        if (!reverseCode.containsKey(aminoAcid)) {
            return null;
        }
        return reverseCode.get(aminoAcid);
    }

    /**
     * returns a list of triples producing the same amino acid as the passed triplet
     *
     * @param triplet the triplet
     * @return a list of triplets with similar amino acids to triplet
     */
    public static List<String> triplesWithSimilarAminoAcid(String triplet) {
        return triplesByAminoAcid(aminoAcidByTriplet(triplet));
    }

}
