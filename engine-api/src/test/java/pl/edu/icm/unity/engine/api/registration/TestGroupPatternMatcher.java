/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class TestGroupPatternMatcher
{
	@Test
	public void shouldMatchWithSingleStar()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/*/users"), is(true));
	}

	@Test
	public void shouldMatchWithSingleStarPartial()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/*o/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/f*o/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/g*/users"), is(false));
	}

	@Test
	public void shouldMatchWithDoubleStarInMiddle()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/users", "/group/**/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/users", "/group/**/users"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/baz/users", "/group/**/users"), is(true));

		assertThat(GroupPatternMatcher.matches("/group/foo/bar/baz", "/group/**/users"), is(false));
	}

	@Test
	public void shouldMatchWithDoubleStarTrailing()
	{
		assertThat(GroupPatternMatcher.matches("/group/foo/bar/users", "/group/**"), is(true));
		assertThat(GroupPatternMatcher.matches("/group/foo", "/group/**"), is(true));
	}
	
	@Test
	public void shouldDisallowEmptyGroupElement()
	{
		assertThat(GroupPatternMatcher.matches("/group/users", "/group/*/users"), is(false));
		assertThat(GroupPatternMatcher.matches("/group", "/group/**"), is(true));
	}
}
