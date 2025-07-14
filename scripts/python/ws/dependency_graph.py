#!/usr/bin/env python3

import re
import subprocess as sp
import webbrowser
from pathlib import Path

import execution_utils
import workspace
from formatting_utils import bold, red, yellow, green
from iter_utils import PeekIterator
from program_utils import exit_if_task_failed


class DependencyGraph:
    @staticmethod
    def filtered_by_prefix(root_package, prefix):
        return DependencyGraph.all_packages(root_package).filter_nodes(lambda node: node.startswith(prefix))


    @staticmethod
    def checked_packages_parents(root_package):
        graph = DependencyGraph.all_packages(root_package)
        leaf_packages = [graph[pkg.name] for pkg in workspace.Context.ws.checked_packages() if pkg.name in graph.nodes.keys()]
        packages_to_keep = set()

        while len(leaf_packages) > 0:
            node = leaf_packages.pop()
            packages_to_keep.add(node.name)
            leaf_packages.extend(node.parents)

        return graph.filter_nodes(lambda name: name in packages_to_keep)


    @staticmethod
    def all_packages(root_package):
        print(bold(yellow('Calculating dependency graph for {}...'.format(root_package))))

        cwd = root_package.path()
        task_result = execution_utils.exec_cmd('TODO', stdout=sp.PIPE, cwd=cwd)
        exit_if_task_failed(task_result, additional_message='Failed to generate dependency graph')

        print(bold(green('Successfully determined dependency graph for {}'.format(root_package))))
        dot_path = task_result.result().strip()
        checked_packages = {pkg.name for pkg in workspace.Context.ws.checked_packages()}
        return Digraph.parse(dot_path).paint_nodes(checked_packages, 'lightblue')


    def __init__(self, graph):
        self.graph = graph


class Digraph:
    def __init__(self):
        self.dot_elements = []
        self.nodes = {}

    @staticmethod
    def parse(dot_file):
        graph = Digraph()

        with open(dot_file, 'r') as dot:
            lines = PeekIterator(iter(dot.readlines()))
            while lines.peek() is not None:
                graph.add(Digraph.parse_next_element(lines))

        return graph


    @staticmethod
    def parse_next_element(lines):
        line = next(lines).strip()

        match = re.fullmatch(DotNode.NODE_PATTERN, line)
        if match is not None:
            return DotNode(match.group('package'), match.group('version'), match.group('source'), match.group('style'))

        match = re.fullmatch(DotEdge.EDGE_PATTERN, line)
        if match is not None:
            sub_match = re.fullmatch(DotEdge.EDGE_METADATA_PATTERN, lines.peek().strip())
            if sub_match is not None:
                next(lines)
                return DotEdge(match.group('from'), match.group('from_version'), match.group('to'),
                               match.group('to_version'), sub_match.group('source'), sub_match.group('style'))

        return PlainText(line)


    def add(self, element):
        self.dot_elements.append(element)

        if type(element) == DotNode:
            self._create_node_if_necessary(element.name)

        elif type(element) == DotEdge:
            self._create_node_if_necessary(element.from_node)
            self._create_node_if_necessary(element.to_node)
            self[element.from_node].depends_on(self[element.to_node])


    def _create_node_if_necessary(self, name):
        if name not in self.nodes:
            self.nodes[name] = GraphNode(name)


    def filter_nodes(self, predicate):
        filtered = Digraph()
        for element in self.dot_elements:
            if type(element) == PlainText or \
              (type(element) == DotNode and predicate(element.name)) or \
              (type(element) == DotEdge and predicate(element.from_node) and predicate(element.to_node)):

                filtered.add(element)

        return filtered


    def paint_nodes(self, names, color):
        repainted = Digraph()
        for elt in self.dot_elements:
            if type(elt) == DotNode and elt.name in names:
                new_style = elt.style.copy()
                new_style['fillcolor'] = color
                new_style['style'] = 'filled'

                repainted.add(DotNode(elt.name, elt.version, elt.source, 'fillcolor="{}", style="filled"'.format(color)))
            else:
                repainted.add(elt)

        return repainted


    def show(self, dot_path):
        self.write_to_file(dot_path)

        task_result = execution_utils.exec_cmd('dot -Tsvg -O {}'.format(dot_path), cwd=str(Path(dot_path).parent))
        if task_result.is_failure():
            print(bold(red('Failed to generate dependency graph with "{}"'.format(task_result.cmd))))
        else:
            print('Opening SVG output using default program...')
            webbrowser.open('file:///{}.svg'.format(dot_path), new=0, autoraise=True)


    def write_to_file(self, path):
        with open(path, 'w') as file:
            for i, element in enumerate(self.dot_elements):
                if i != 0 and i != len(self.dot_elements) - 1:
                    file.write('  ')

                file.write(str(element))
                file.write('\n')


    def __getitem__(self, key):
        return self.nodes[key]


    def __contains__(self, key):
        return key in self.nodes


class GraphNode:
    def __init__(self, name):
        self.name = name
        self.dependencies = set()
        self.parents = set()


    def depends_on(self, node):
        self.dependencies.add(node)
        node.parents.add(self)


    def is_root(self):
        return len(self.parents) == 0


    def is_leaf(self):
        return len(self.dependencies) == 0


    def __str__(self):
        return self.__repr__()


    def __repr__(self):
        return 'GraphNode({})'.format(self.name)


class DotElement:
    def __init__(self):
        pass


class PlainText(DotElement):
    def __init__(self, content):
        super().__init__()
        self.content = content


    def __repr__(self):
        return "PlaintText({})".format(self.content)


    def __str__(self):
        return self.content


class DotNode(DotElement):
    NODE_PATTERN = re.compile('"(?P<package>.+)?-(?P<version>.+)"( /\*(?P<source>.+)\*/)?( \[(?P<style>.*)\])?;')

    def __init__(self, name, version, source=None, style=None):
        super().__init__()
        self.name = name
        self.version = version
        self.source = source
        self.style = DotStyle.parse(style)


    def __repr__(self):
        return "Node({}, {}, {}, {})".format(self.name, self.version, self.source, self.style)


    def __str__(self):
        return '{}{}{};'.format(_format_node(self.name, self.version), _format_source(self.source), _format_style(self.style))


class DotEdge(DotElement):
    EDGE_PATTERN = re.compile('"(?P<from>.+)?-(?P<from_version>.+)" -> "(?P<to>.+)?-(?P<to_version>.+)"')
    EDGE_METADATA_PATTERN = re.compile('(/\*(?P<source>.+)\*/)?( \[(?P<style>.*)\])?;')

    def __init__(self, from_node, from_version, to_node, to_version, source, style):
        super().__init__()
        self.from_node = from_node
        self.from_version = from_version
        self.to_node = to_node
        self.to_version = to_version
        self.source = source
        self.style = DotStyle.parse(style)

    def __repr__(self):
        return "Edge({}, {}, {}, {}, {}, {})".format(self.from_node, self.from_version, self.to_node, self.to_version, self.source, self.style)


    def __str__(self):
        formatted_from = _format_node(self.from_node, self.from_version)
        formatted_to = _format_node(self.to_node, self.to_version)
        return '{} -> {}\n    {}{};'.format(formatted_from, formatted_to, _format_source(self.source), _format_style(self.style))


class DotStyle:
    STYLE_PROPERTY_PATTERN = re.compile('(\w+?)="(.+?)"')

    @staticmethod
    def parse(style):
        if style is None:
            return DotStyle({})

        matches = re.findall(DotStyle.STYLE_PROPERTY_PATTERN, style)
        return DotStyle({m[0]: m[1] for m in matches})


    def copy(self):
        return DotStyle(self.properties.copy())


    def __init__(self, properties):
        self.properties = properties


    def __getitem__(self, key):
        return self.properties[key]


    def __setitem__(self, key, value):
        res = self.properties[key] = value
        return res

    def __repr__(self):
        return "Style({})".format(self.properties)


    def __str__(self):
        return ', '.join(['{} = "{}"'.format(k, v) for k, v in self.properties.items()])


def _format_node(name, version=None):
    version = '' if version is None else '-' + version
    return '"{}{}"'.format(name, version)


def _format_source(source):
    return '' if source is None else ' /*{}*/'.format(source)


def _format_style(style):
    return '' if len(style.properties) == 0 else ' [{}]'.format(str(style))
