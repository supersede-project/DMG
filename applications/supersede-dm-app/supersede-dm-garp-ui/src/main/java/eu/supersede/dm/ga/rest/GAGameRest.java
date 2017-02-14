package eu.supersede.dm.ga.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.supersede.dm.ga.GAPersistentDB;
import eu.supersede.dm.iga.IGAAlgorithm;
import eu.supersede.fe.security.DatabaseUser;
import eu.supersede.gr.jpa.RequirementsJpa;
import eu.supersede.gr.jpa.ValutationCriteriaJpa;
import eu.supersede.gr.model.HGAGameSummary;
import eu.supersede.gr.model.Requirement;
import eu.supersede.gr.model.ValutationCriteria;

@RestController
@RequestMapping("/garp/game")
public class GAGameRest
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RequirementsJpa requirementsJpa;

    @Autowired
    private ValutationCriteriaJpa criteriaJpa;

    @Autowired
    private GAPersistentDB persistentDB;

    @RequestMapping(value = "/ownedgames", method = RequestMethod.GET)
    public List<HGAGameSummary> getOwnedGames(Authentication authentication)
    {
        return persistentDB.getOwnedGames(((DatabaseUser) authentication.getPrincipal()).getUserId());
    }

    @RequestMapping(value = "/activegames", method = RequestMethod.GET)
    public List<HGAGameSummary> getActiveGames(Authentication authentication)
    {
        return persistentDB.getActiveGames(((DatabaseUser) authentication.getPrincipal()).getUserId());
    }

    @RequestMapping(value = "/newgame", method = RequestMethod.POST)
    public void createNewGame(Authentication authentication, @RequestParam Long[] gameRequirements,
            @RequestBody Map<Long, Double> gameCriteriaWeights, @RequestParam Long[] gameOpinionProviders,
            @RequestParam Long[] gameNegotiators)
    {
        persistentDB.create(authentication, gameRequirements, gameCriteriaWeights, gameOpinionProviders,
                gameNegotiators);
    }

    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public void submitAllPriorities(Authentication authentication, @RequestParam Long gameId,
            @RequestBody Map<Long, List<Long>> reqs)
    {
        Long userId = ((DatabaseUser) authentication.getPrincipal()).getUserId();

        for (Long key : reqs.keySet())
        {
            log.info("Sending priorities: game " + gameId + " user " + userId + " criterion " + key + " reqs "
                    + reqs.get(key));
        }

        persistentDB.setRanking(gameId, userId, reqs);
    }

    @RequestMapping(value = "/ranking", method = RequestMethod.POST)
    public List<Long> getRanking(@RequestParam Long gameId, @RequestParam Long userId, @RequestParam Long criterionId)
    {
        return persistentDB.getRankingsCriterion(gameId, userId, criterionId);
    }

    @RequestMapping(value = "/requirements", method = RequestMethod.GET)
    public List<Requirement> getRequirements(Authentication authentication, Long gameId, Long criterion)
    {
        Long userId = ((DatabaseUser) authentication.getPrincipal()).getUserId();
        List<Long> reqs = persistentDB.getRankingsCriterion(gameId, userId, criterion);
        List<Requirement> requirements = new ArrayList<>();

        if (reqs.size() > 0)
        {
            for (Long requirementId : reqs)
            {
                requirements.add(requirementsJpa.findOne(requirementId));
            }
        }
        else
        {
            List<Long> tmp = persistentDB.getRequirements(gameId);

            for (Long requirementId : tmp)
            {
                requirements.add(requirementsJpa.findOne(requirementId));
            }
        }

        return requirements;
    }

    @RequestMapping(value = "/game", method = RequestMethod.GET)
    public HGAGameSummary getGame(Authentication authentication, Long gameId)
    {
        return persistentDB.getGameInfo(gameId).getGame();
    }

    @RequestMapping(value = "/gamecriteria", method = RequestMethod.GET)
    public List<ValutationCriteria> getGameCriteria(Authentication authentication, Long gameId)
    {
        List<ValutationCriteria> criteria = new ArrayList<>();
        Set<Long> criteriaIds = persistentDB.getCriteriaWeights(gameId).keySet();

        for (Long criterionId : criteriaIds)
        {
            criteria.add(criteriaJpa.findOne(criterionId));
        }

        return criteria;
    }

    @RequestMapping(value = "/gamecriterion", method = RequestMethod.GET)
    public ValutationCriteria getGameCriterion(Authentication authentication, Long criterionId)
    {
        return criteriaJpa.findOne(criterionId);
    }

    @RequestMapping(value = "/requirement", method = RequestMethod.GET)
    public Requirement getRequirement(Authentication authentication, Long requirementId)
    {
        return requirementsJpa.findOne(requirementId);
    }

    @RequestMapping(value = "/gamerequirements", method = RequestMethod.GET)
    public List<Requirement> getGameRequirements(Authentication authentication, Long gameId)
    {
        List<Requirement> requirements = new ArrayList<>();
        List<Long> requirementsId = persistentDB.getRequirements(gameId);

        for (Long requirementId : requirementsId)
        {
            requirements.add(requirementsJpa.findOne(requirementId));
        }

        return requirements;
    }

    @RequestMapping(value = "/calc", method = RequestMethod.GET)
    public List<Map<String, Double>> calcRanking(Authentication authentication, Long gameId)
    {
        IGAAlgorithm algo = new IGAAlgorithm();
        HashMap<Long, Double> criteriaWeights = persistentDB.getCriteriaWeights(gameId);
        List<String> gameCriteria = new ArrayList<>();

        for (Long criterionId : criteriaWeights.keySet())
        {
            gameCriteria.add("" + criterionId);
            algo.setCriterionWeight("" + criterionId, criteriaWeights.get(criterionId));
        }

        algo.setCriteria(gameCriteria);

        for (Long requirementId : persistentDB.getRequirements(gameId))
        {
            algo.addRequirement("" + requirementId, new ArrayList<>());
        }

        // get all the players in this game
        List<Long> participantIds = persistentDB.getParticipants(gameId);

        // get the rankings of each player for each criterion
        List<String> players = new ArrayList<>();

        for (Long userId : participantIds)
        {
            String player = "" + userId;
            players.add(player);
            Map<Long, List<Long>> userRanking = persistentDB.getRanking(gameId, userId);

            if (userRanking != null)
            {
                Map<String, List<String>> userRankingStr = new HashMap<>();

                for (Entry<Long, List<Long>> entry : userRanking.entrySet())
                {
                    List<String> requirements = new ArrayList<>();

                    for (Long requirement : entry.getValue())
                    {
                        requirements.add("" + requirement);
                    }

                    userRankingStr.put("" + entry.getKey(), requirements);
                }

                algo.addRanking(player, userRankingStr);
            }
        }

        algo.setDefaultPlayerWeights(gameCriteria, players);

        List<Map<String, Double>> prioritizations = null;

        try
        {
            prioritizations = algo.calc().subList(0, 3);
        }
        catch (Exception e)
        {
            log.error("Unable to compute prioritizations!");
            e.printStackTrace();
            prioritizations = new ArrayList<>();
        }

        return prioritizations;
    }
}