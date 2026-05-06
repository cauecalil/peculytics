<script lang="ts">
	import './layout.css';
	import favicon from '$lib/assets/favicon.svg';
	import { page } from '$app/stores';
	import NotificationHost from '$lib/components/NotificationHost.svelte';

	let { children } = $props();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Peculytics</title>
	<meta name="description" content="AI-powered bank statement analysis." />
</svelte:head>

<div class="relative min-h-screen overflow-hidden">
	<div class="pointer-events-none absolute inset-0 -z-10">
		<div
			class="absolute -left-24 top-24 h-72 w-72 rounded-full bg-[color:var(--accent)]/20 blur-3xl animate-float"
		></div>
		<div
			class="absolute right-[-120px] top-10 h-80 w-80 rounded-full bg-[color:var(--teal)]/20 blur-3xl animate-float"
		></div>
		<div
			class="absolute bottom-[-140px] left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-[color:var(--rose)]/20 blur-3xl animate-float"
		></div>
	</div>

	<header
		class="mx-auto flex max-w-6xl flex-col gap-6 px-6 pb-6 pt-10 sm:flex-row sm:items-center sm:justify-between"
	>
		<a href="/" class="text-3xl font-semibold tracking-tight">Peculytics</a>
		<nav class="flex items-center gap-3 text-sm">
			<a
				href="/"
				class={`btn-ghost ${$page.url.pathname === '/' ? 'ring-2 ring-[color:var(--accent)]' : ''}`}
			>
				Analyses
			</a>
			<a href="/analyses/new" class="btn-primary">New Analysis</a>
		</nav>
	</header>

	<main class="mx-auto flex max-w-6xl flex-col gap-10 px-6 pb-20">
		{@render children()}
	</main>
	<NotificationHost />
</div>
