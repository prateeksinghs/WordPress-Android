package org.wordpress.android.ui.stats.refresh.lists.sections.insights.blocks

import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.fluxc.store.InsightsStore.OnInsightsFetched
import org.wordpress.android.fluxc.store.InsightsStore.StatsError
import org.wordpress.android.fluxc.store.InsightsStore.StatsErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Label
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Link
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.TabsItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Title
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LABEL
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.LIST_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.TITLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.Type.USER_ITEM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.UserItem
import org.wordpress.android.ui.stats.refresh.lists.Failed
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.FAILED
import org.wordpress.android.ui.stats.refresh.lists.StatsListItem.Type.BLOCK_LIST
import org.wordpress.android.ui.stats.refresh.lists.BlockList

class CommentsBlockTest : BaseUnitTest() {
    @Mock lateinit var insightsStore: InsightsStore
    @Mock lateinit var site: SiteModel
    private lateinit var useCase: CommentsBlock
    private val postId: Long = 10
    private val postTitle = "Post"
    private val avatar = "avatar.jpg"
    private val user = "John Smith"
    private val url = "www.url.com"
    private val totalCount = 50
    private val pageSize = 6
    @Before
    fun setUp() {
        useCase = CommentsBlock(
                Dispatchers.Unconfined,
                insightsStore
        )
    }

    @Test
    fun `maps posts comments to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        CommentsModel(
                                listOf(CommentsModel.Post(postId, postTitle, totalCount, url)),
                                listOf(),
                                hasMorePosts = false,
                                hasMoreAuthors = false
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            val tabsItem = this.items[1] as TabsItem

            assertThat(tabsItem.tabs[0].title).isEqualTo(string.stats_comments_authors)
            assertThat(tabsItem.tabs[0].items).containsOnly(Empty)

            assertThat(tabsItem.tabs[1].title).isEqualTo(string.stats_comments_posts_and_pages)
            assertTabWithPosts(tabsItem.tabs[1])
        }
    }

    @Test
    fun `adds link to UI model when has more posts`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        CommentsModel(
                                listOf(),
                                listOf(),
                                hasMorePosts = true,
                                hasMoreAuthors = false
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(3)
            assertTitle(this.items[0])
            assertThat(this.items[2] is Link).isTrue()
        }
    }

    @Test
    fun `adds link to UI model when has more authors`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        CommentsModel(
                                listOf(),
                                listOf(),
                                hasMorePosts = false,
                                hasMoreAuthors = true
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(3)
            assertTitle(this.items[0])
            assertThat(this.items[2] is Link).isTrue()
        }
    }

    @Test
    fun `maps comment authors to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        CommentsModel(
                                listOf(),
                                listOf(CommentsModel.Author(user, totalCount, url, avatar)),
                                hasMorePosts = false,
                                hasMoreAuthors = false
                        )
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            val tabsItem = this.items[1] as TabsItem

            assertThat(tabsItem.tabs[0].title).isEqualTo(string.stats_comments_authors)
            assertTabWithUsers(tabsItem.tabs[0])

            assertThat(tabsItem.tabs[1].title).isEqualTo(string.stats_comments_posts_and_pages)
            assertThat(tabsItem.tabs[1].items).containsOnly(Empty)
        }
    }

    @Test
    fun `maps empty comments to UI model`() = test {
        val forced = false
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        CommentsModel(listOf(), listOf(), hasMorePosts = false, hasMoreAuthors = false)
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(BLOCK_LIST)
        (result as BlockList).apply {
            assertThat(this.items).hasSize(2)
            assertTitle(this.items[0])
            val tabsItem = this.items[1] as TabsItem

            assertThat(tabsItem.tabs[0].title).isEqualTo(string.stats_comments_authors)
            assertThat(tabsItem.tabs[0].items).containsOnly(Empty)

            assertThat(tabsItem.tabs[1].title).isEqualTo(string.stats_comments_posts_and_pages)
            assertThat(tabsItem.tabs[1].items).containsOnly(Empty)
        }
    }

    @Test
    fun `maps error item to UI model`() = test {
        val forced = false
        val message = "Generic error"
        whenever(insightsStore.fetchComments(site, pageSize, forced)).thenReturn(
                OnInsightsFetched(
                        StatsError(GENERIC_ERROR, message)
                )
        )

        val result = loadComments(true, forced)

        assertThat(result.type).isEqualTo(FAILED)
        (result as Failed).apply {
            assertThat(this.failedType).isEqualTo(string.stats_view_comments)
            assertThat(this.errorMessage).isEqualTo(message)
        }
    }

    private fun assertTabWithPosts(tab: TabsItem.Tab) {
        val labelItem = tab.items[0]
        assertThat(labelItem.type).isEqualTo(LABEL)
        assertThat((labelItem as Label).leftLabel).isEqualTo(R.string.stats_comments_title_label)
        assertThat(labelItem.rightLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = tab.items[1]
        assertThat(userItem.type).isEqualTo(LIST_ITEM)
        assertThat((userItem as ListItem).text).isEqualTo(postTitle)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
    }

    private fun assertTabWithUsers(tab: TabsItem.Tab) {
        val labelItem = tab.items[0]
        assertThat(labelItem.type).isEqualTo(LABEL)
        assertThat((labelItem as Label).leftLabel).isEqualTo(R.string.stats_comments_author_label)
        assertThat(labelItem.rightLabel).isEqualTo(R.string.stats_comments_label)

        val userItem = tab.items[1]
        assertThat(userItem.type).isEqualTo(USER_ITEM)
        assertThat((userItem as UserItem).avatarUrl).isEqualTo(avatar)
        assertThat(userItem.showDivider).isEqualTo(false)
        assertThat(userItem.text).isEqualTo(user)
        assertThat(userItem.value).isEqualTo(totalCount.toString())
    }

    private fun assertTitle(item: BlockListItem) {
        assertThat(item.type).isEqualTo(TITLE)
        assertThat((item as Title).text).isEqualTo(R.string.stats_view_comments)
    }

    private suspend fun loadComments(refresh: Boolean, forced: Boolean): StatsListItem {
        var result: StatsListItem? = null
        useCase.liveData.observeForever { result = it }
        useCase.fetch(site, refresh, forced)
        return checkNotNull(result)
    }
}
