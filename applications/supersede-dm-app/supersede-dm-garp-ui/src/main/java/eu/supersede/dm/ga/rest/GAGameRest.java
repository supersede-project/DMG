/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package eu.supersede.dm.ga.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import eu.supersede.gr.data.GAGameDetails;
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

    @RequestMapping(value = "/games", method = RequestMethod.GET)
    public List<HGAGameSummary> getGames(Authentication authentication, String roleName)
    {
        Long userId = ((DatabaseUser) authentication.getPrincipal()).getUserId();
        return persistentDB.getGamesByRole(userId, roleName);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/newgame", method = RequestMethod.POST)
    public void createNewGame(Authentication authentication, @RequestParam String name,
            @RequestParam Long[] gameRequirements, @RequestBody Map<String, ?> weights,
            @RequestParam Long[] gameOpinionProviders, @RequestParam Long[] gameNegotiators)
    {
        String criteriaKey = "criteria";
        String playersKey = "players";
        Map<String, Double> criteriaWeights;
        Map<String, Map<String, Double>> playersWeights = null;

        if (!weights.containsKey(criteriaKey))
        {
            log.error("Missing weights for criteria to create a new game");
            return;
        }

        if (weights.get(criteriaKey) instanceof Map<?, ?>)
        {
            criteriaWeights = (Map<String, Double>) weights.get(criteriaKey);
        }
        else
        {
            log.error("Wrong type for criteria weights: expected Map<String, Double>, found "
                    + weights.get(criteriaKey).getClass().getName());
            return;
        }

        if (!weights.containsKey(playersKey))
        {
            log.error("Missing weights for players to create a new game");
            return;
        }

        if (weights.get(playersKey) instanceof Map<?, ?>)
        {
            playersWeights = (Map<String, Map<String, Double>>) weights.get(playersKey);
        }
        else
        {
            log.error("Wrong type for players weights: expected Map<String, Map<Long, Double>>, found "
                    + weights.get(playersKey).getClass().getName());
            return;
        }

        persistentDB.create(authentication, name, gameRequirements, playersWeights, criteriaWeights,
                gameOpinionProviders, gameNegotiators);
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

    @RequestMapping(value = "/ranking", method = RequestMethod.GET)
    public Map<Long, Map<Long, List<Long>>> getRanking(@RequestParam Long gameId)
    {
        return persistentDB.getGameInfo(gameId).getRankings();
    }

    @RequestMapping(value = "/userranking", method = RequestMethod.GET)
    public Map<Long, List<Long>> getUserRanking(Authentication authentication, @RequestParam Long gameId)
    {
        Long userId = ((DatabaseUser) authentication.getPrincipal()).getUserId();
        GAGameDetails gameDetails = persistentDB.getGameInfo(gameId);

        if (!gameDetails.getOpinionProviders().contains(userId))
        {
            return new HashMap<>();
        }
        else
        {
            return persistentDB.getRanking(gameId, userId);
        }
    }

    @RequestMapping(value = "/solution", method = RequestMethod.POST)
    public void selectSolution(@RequestParam Long gameId, @RequestBody List<Long> solution)
    {
        persistentDB.selectSolution(gameId, solution);
    }

    @RequestMapping(value = "/solution", method = RequestMethod.GET)
    public List<Long> getSolution(@RequestParam Long gameId)
    {
        return persistentDB.getSolution(gameId);
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

    @RequestMapping(value = "/closegame", method = RequestMethod.POST)
    public void closeGame(Authentication authentication, Long gameId)
    {
        persistentDB.closeGame(gameId);
    }

    @RequestMapping(value = "/opengame", method = RequestMethod.POST)
    public void openGame(Authentication authentication, Long gameId)
    {
        persistentDB.openGame(gameId);
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
    public List<List<String>> calcRanking(Authentication authentication, Long gameId)
    {
        IGAAlgorithm algo = new IGAAlgorithm();
        Map<Long, Map<Long, Double>> playerWeights = persistentDB.getPlayerWeights(gameId);
        Map<Long, Double> criteriaWeights = persistentDB.getCriteriaWeights(gameId);
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
        List<Long> participantIds = persistentDB.getOpinionProviders(gameId);

        // get the rankings of each player for each criterion
        List<String> players = new ArrayList<>();

        // Get opinion providers that have submitted their rankings
        List<Long> votedPlayers = new ArrayList<>();

        for (Long userId : participantIds)
        {
            System.out.println("Getting ranking of user " + userId);
            String player = "" + userId;
            players.add(player);
            Map<Long, List<Long>> userRanking = persistentDB.getRanking(gameId, userId);

            if (userRanking.keySet().size() > 0)
            {
                votedPlayers.add(userId);
                Map<String, List<String>> userRankingStr = new HashMap<>();

                for (Long criterionId : userRanking.keySet())
                {
                    System.out.println("Criterion " + criterionId);
                    List<String> requirements = new ArrayList<>();

                    for (Long requirement : userRanking.get(criterionId))
                    {
                        System.out.println("Adding requirement: " + requirement);
                        requirements.add("" + requirement);
                    }

                    userRankingStr.put("" + criterionId, requirements);
                }

                algo.addRanking(player, userRankingStr);
            }
        }

        for (Long criterionId : playerWeights.keySet())
        {
            Map<Long, Double> criterionPlayerWeights = playerWeights.get(criterionId);
            Map<String, Double> algorithmPlayerWeights = new HashMap<>();

            for (Long userId : criterionPlayerWeights.keySet())
            {
                // Add the player weight only if he submitted the rankings
                if (votedPlayers.contains(userId))
                {
                    algorithmPlayerWeights.put("" + userId, criterionPlayerWeights.get(userId));
                }
            }

            algo.setPlayerWeights("" + criterionId, algorithmPlayerWeights);
        }

        List<Map<String, Double>> solutions = null;

        try
        {
            solutions = algo.calc();
        }
        catch (Exception e)
        {
            log.error("Unable to compute prioritizations!");
            e.printStackTrace();
            solutions = new ArrayList<>();
        }

        List<Map<String, Double>> solutionSubset = null;

        if (solutions.size() > 3)
        {
            solutionSubset = solutions.subList(0, 3);
        }
        else
        {
            solutionSubset = solutions;
        }

        List<List<String>> prioritizations = new ArrayList<>();

        for (Map<String, Double> solution : solutionSubset)
        {
            List<String> prioritization = new ArrayList<>();

            for (String requirement : solution.keySet())
            {
                prioritization.add(requirement);
            }

            prioritizations.add(prioritization);
        }

        return prioritizations;
    }
}