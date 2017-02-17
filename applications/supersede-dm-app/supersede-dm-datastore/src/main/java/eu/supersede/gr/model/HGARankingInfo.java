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

package eu.supersede.gr.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@IdClass(RankingId.class)
@Table(name = "h_ga_rankings")
public class HGARankingInfo
{
    @Id
    private Long gameId;

    @Id
    private Long userId;

    @Column(columnDefinition = "varchar(5000)")
    private String jsonizedRanking;

    public Long getGameId()
    {
        return gameId;
    }

    public void setGameId(Long gameId)
    {
        this.gameId = gameId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getJsonizedRanking()
    {
        return jsonizedRanking;
    }

    public void setJsonizedRanking(String jsonizedRanking)
    {
        this.jsonizedRanking = jsonizedRanking;
    }
}