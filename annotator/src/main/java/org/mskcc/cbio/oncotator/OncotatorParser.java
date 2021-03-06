/** Copyright (c) 2012 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center 
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center 
 * has been advised of the possibility of such damage.
*/

package org.mskcc.cbio.oncotator;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Parses JSON Retrieved from Oncotator.
 *
 * @author Selcuk Onur Sumer
 */
public class OncotatorParser
{

	/**
	 * Parses the JSON returned by the oncotator web service, and returns
	 * the information as a new OncotateRecord instance.
	 * 
	 * @param key   chr#_start_end_allele1_allele2
	 * @param json  JSON object returned by the web service
	 * @return      new OncotatorRecord, or null if JSON has an error
	 */
    public static OncotatorRecord parseJSON(String key, String json)
    {
        ObjectMapper m = new ObjectMapper();
	    JsonNode rootNode = null;

	    // check for invalid json value
	    if (json == null)
	    {
		    return null;
	    }

	    // return null if cannot read root node
	    try {
		    rootNode = m.readValue(json, JsonNode.class);
	    }
	    catch (IOException e) {
		    //e.printStackTrace();
		    return null;
	    }

	    OncotatorRecord oncoRecord = new OncotatorRecord(key);
        oncoRecord.setRawJson(json);

        // check if JSON has an ERROR
        
        JsonNode errorNode = rootNode.path("ERROR");
        if (!errorNode.isMissingNode())
        {
        	System.out.println("JSON parse error for " + key + ": " + errorNode.getTextValue());
        	return null;
        }

        // proceed in case of no JSON error
        
        JsonNode genomeChange = rootNode.path("genome_change");
        if (!genomeChange.isMissingNode()) {
            oncoRecord.setGenomeChange(genomeChange.getTextValue());
        }

        JsonNode cosmic = rootNode.path("Cosmic_overlapping_mutations");
        if (!cosmic.isMissingNode()) {
            oncoRecord.setCosmicOverlappingMutations(cosmic.getTextValue());
        }

        JsonNode dbSnpRs = rootNode.path("dbSNP_RS");
        if (!dbSnpRs.isMissingNode()) {
            oncoRecord.setDbSnpRs(dbSnpRs.getTextValue());
        }

	    JsonNode dbSnpValStatus = rootNode.path("dbSNP_Val_Status");
	    if (!dbSnpValStatus.isMissingNode()) {
		    oncoRecord.setDbSnpValStatus(dbSnpValStatus.getTextValue());
	    }

	    Transcript bestCanonical = findBestCanonical(rootNode);
	    Transcript bestEffect = findBestEffect(rootNode);

	    if (bestCanonical != null)
	    {
	        oncoRecord.setBestCanonicalTranscript(bestCanonical);
	    }

	    if (bestEffect != null)
	    {
		    oncoRecord.setBestEffectTranscript(bestEffect);
	    }

        return oncoRecord;
    }

	/**
	 * Determines the best canonical transcript for the given root node.
	 *
	 * @param rootNode  root node representing the whole JSON
	 * @return          best canonical transcript for this mutation
	 */
	public static Transcript findBestCanonical(JsonNode rootNode)
	{
		// TODO do not use index provided by oncotator, define own mapping
		JsonNode transcriptIdxNode = rootNode.path("best_canonical_transcript");
		Transcript transcript = null;

		if (!transcriptIdxNode.isMissingNode())
		{
			int transcriptIndex = transcriptIdxNode.getIntValue();
			JsonNode transcriptsNode = rootNode.path("transcripts");
			transcript = parseTranscriptNode(transcriptsNode, transcriptIndex);
		}

		return transcript;
	}

	/**
	 * Determines the best effect transcript for the given root node.
	 *
	 * @param rootNode  root node representing the whole JSON
	 * @return          best effect transcript for this mutation
	 */
	public static Transcript findBestEffect(JsonNode rootNode)
	{
		// TODO do not use index provided by oncotator, define own mapping
		JsonNode transcriptIdxNode = rootNode.path("best_effect_transcript");
		Transcript transcript = null;

		if (!transcriptIdxNode.isMissingNode())
		{
			int transcriptIndex = transcriptIdxNode.getIntValue();
			JsonNode transcriptsNode = rootNode.path("transcripts");
			transcript = parseTranscriptNode(transcriptsNode, transcriptIndex);
		}

		return transcript;
	}

	/**
	 * Parses a transcript node at the specified index within the given
	 * transcripts node.
	 *
	 * @param transcriptsNode   node containing all transcripts
	 * @param transcriptIndex   specific index for a single transcript
	 * @return                  Transcript instance containing parsed info
	 */
	public static Transcript parseTranscriptNode(JsonNode transcriptsNode,
			int transcriptIndex)
	{
		// get the transcript node for the specified index
		JsonNode transcriptNode = transcriptsNode.get(transcriptIndex);

		// parse nodes for the transcript
		JsonNode variantClassification = transcriptNode.path("variant_classification");
		JsonNode proteinChange = transcriptNode.path("protein_change");
		JsonNode geneSymbol = transcriptNode.path("gene");
		JsonNode exonAffected = transcriptNode.path("exon_affected");
		JsonNode refseqMrnaId = transcriptNode.path("refseq_mRNA_id");
		JsonNode refseqProtId = transcriptNode.path("refseq_prot_id");
		JsonNode uniprotName = transcriptNode.path("uniprot_entry_name");
		JsonNode uniprotAccession = transcriptNode.path("uniprot_accession");
		JsonNode codonChange = transcriptNode.path("codon_change");
		JsonNode transcriptChange = transcriptNode.path("transcript_change");
		JsonNode proteinPosStart = transcriptNode.path("protein_position_start");
		JsonNode proteinPosEnd = transcriptNode.path("protein_position_end");

		// construct a transcript instance for the parsed nodes

		Transcript transcript = new Transcript();

		if (!variantClassification.isMissingNode())
		{
			transcript.setVariantClassification(variantClassification.getTextValue());
		}

		if (!proteinChange.isMissingNode())
		{
			transcript.setProteinChange(proteinChange.getTextValue());
		}

		if (!geneSymbol.isMissingNode())
		{
			transcript.setGene(geneSymbol.getTextValue());
		}

		if (!exonAffected.isMissingNode())
		{
			transcript.setExonAffected(exonAffected.getIntValue());
		}

		if (!refseqMrnaId.isMissingNode())
		{
			transcript.setRefseqMrnaId(refseqMrnaId.getTextValue());
		}

		if (!refseqProtId.isMissingNode())
		{
			transcript.setRefseqProtId(refseqProtId.getTextValue());
		}

		if (!uniprotName.isMissingNode())
		{
			transcript.setUniprotName(uniprotName.getTextValue());
		}

		if (!uniprotAccession.isMissingNode())
		{
			transcript.setUniprotAccession(uniprotAccession.getTextValue());
		}

		if (!codonChange.isMissingNode())
		{
			transcript.setCodonChange(codonChange.getTextValue());
		}

		if (!transcriptChange.isMissingNode())
		{
			transcript.setTranscriptChange(transcriptChange.getTextValue());
		}

		if (!proteinPosStart.isMissingNode())
		{
			transcript.setProteinPosStart(proteinPosStart.getIntValue());
		}

		if (!proteinPosEnd.isMissingNode())
		{
			transcript.setProteinPosEnd(proteinPosEnd.getIntValue());
		}

		return transcript;
	}
}
